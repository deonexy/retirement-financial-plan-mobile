package com.pensiunsehat.finansial.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pensiunsehat.finansial.data.backup.BackupImportMode
import com.pensiunsehat.finansial.data.backup.LocalEncryptedBackupManager
import com.pensiunsehat.finansial.data.repository.LocalFinancialRepository
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import com.pensiunsehat.finansial.data.repository.ThemePreference
import com.pensiunsehat.finansial.domain.calculator.formatGoldPriceStatus
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.GoldPriceStatus
import com.pensiunsehat.finansial.worker.BackgroundWorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val financialRepository: LocalFinancialRepository,
    private val backgroundWorkScheduler: BackgroundWorkScheduler,
    private val backupManager: LocalEncryptedBackupManager,
) : ViewModel() {
    private val latestBackupPathState = MutableStateFlow(backupManager.latestBackupFile()?.absolutePath ?: "Belum ada backup terenkripsi")
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        financialRepository.observeGoldPriceSnapshot(),
        latestBackupPathState,
    ) { settings, goldPrice, latestBackupPath ->
        SettingsUiState(
            themePreference = settings.themePreference,
            monthlyReminderEnabled = settings.monthlyReminderEnabled,
            reminderDayOfMonth = settings.reminderDayOfMonth,
            goldPriceAutoRefresh = settings.goldPriceAutoRefresh,
            lastGoldStatus = formatGoldPriceStatus(goldPrice),
            latestBackupPath = latestBackupPath,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(),
    )

    fun setThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch { settingsRepository.setThemePreference(themePreference) }
    }

    fun setMonthlyReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonthlyReminderEnabled(enabled)
            backgroundWorkScheduler.setMonthlyReminderEnabled(enabled, uiState.value.reminderDayOfMonth)
        }
    }

    fun setReminderDayOfMonth(day: Int) {
        viewModelScope.launch {
            val safeDay = day.coerceIn(1, 28)
            settingsRepository.setReminderDayOfMonth(safeDay)
            if (uiState.value.monthlyReminderEnabled) {
                backgroundWorkScheduler.setMonthlyReminderEnabled(true, safeDay)
            }
        }
    }

    fun setGoldPriceAutoRefresh(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGoldPriceAutoRefresh(enabled)
            backgroundWorkScheduler.setGoldPriceAutoRefreshEnabled(enabled)
        }
    }

    fun exportEncryptedBackup() {
        viewModelScope.launch {
            val resultMessage = runCatching { backupManager.exportBackup() }
                .map {
                    latestBackupPathState.value = it.absolutePath
                    "Backup tersimpan: ${it.absolutePath}"
                }
                .getOrElse { "Gagal ekspor backup: ${it.message}" }
            _statusMessage.value = resultMessage
        }
    }

    fun importLatestBackup(mode: BackupImportMode) {
        viewModelScope.launch {
            val resultMessage = runCatching { backupManager.importLatestBackup(mode) }
                .map {
                    latestBackupPathState.value = backupManager.latestBackupFile()?.absolutePath ?: "Belum ada backup terenkripsi"
                    "Impor ${mode.name.lowercase()} berhasil (update=${it.financialUpdateCount}, aset=${it.assetPurchaseCount}, emas=${it.goldSnapshotCount})"
                }
                .getOrElse { "Gagal impor backup: ${it.message}" }
            _statusMessage.value = resultMessage
        }
    }

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage

    fun seedOfflineGoldSnapshot() {
        viewModelScope.launch {
            financialRepository.saveGoldPriceSnapshot(
                GoldPriceSnapshot(
                    pricePerGram = "1900000".toBigDecimal(),
                    capturedAtIso = "2026-09-20",
                    source = "manual-seed",
                    status = GoldPriceStatus.STALE,
                ),
            )
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val financialRepository: LocalFinancialRepository,
        private val backgroundWorkScheduler: BackgroundWorkScheduler,
        private val backupManager: LocalEncryptedBackupManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepository, financialRepository, backgroundWorkScheduler, backupManager) as T
    }
}

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val monthlyReminderEnabled: Boolean = false,
    val reminderDayOfMonth: Int = 1,
    val goldPriceAutoRefresh: Boolean = false,
    val lastGoldStatus: String = "Belum ada snapshot harga emas",
    val latestBackupPath: String = "Belum ada backup terenkripsi",
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    var reminderDayInput by rememberSaveable(state.reminderDayOfMonth) { mutableStateOf(state.reminderDayOfMonth.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Tema")
                ThemePreference.entries.forEach { theme ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = state.themePreference == theme,
                            onClick = { viewModel.setThemePreference(theme) },
                        )
                        Text(theme.name)
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccessibleSwitchRow(
                    label = "Pengingat bulanan via WorkManager",
                    checked = state.monthlyReminderEnabled,
                    onCheckedChange = viewModel::setMonthlyReminder,
                )
                AccessibleSwitchRow(
                    label = "Refresh harga emas otomatis (online)",
                    checked = state.goldPriceAutoRefresh,
                    onCheckedChange = viewModel::setGoldPriceAutoRefresh,
                )
                OutlinedTextField(
                    value = reminderDayInput,
                    onValueChange = { input ->
                        reminderDayInput = input
                        input.toIntOrNull()?.let(viewModel::setReminderDayOfMonth)
                    },
                    label = { Text("Hari pengingat (1-28)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.lastGoldStatus)
                Button(onClick = viewModel::seedOfflineGoldSnapshot, modifier = Modifier.fillMaxWidth()) {
                    Text("Simpan snapshot harga contoh")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.latestBackupPath, style = MaterialTheme.typography.bodySmall)
                Button(onClick = viewModel::exportEncryptedBackup, modifier = Modifier.fillMaxWidth()) {
                    Text("Ekspor backup terenkripsi")
                }
                Button(onClick = { viewModel.importLatestBackup(BackupImportMode.MERGE) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Impor backup terakhir (merge)")
                }
                Button(onClick = { viewModel.importLatestBackup(BackupImportMode.REPLACE) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Impor backup terakhir (replace)")
                }
                if (statusMessage.isNotBlank()) {
                    Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Auth/server tetap dipisahkan. Kredensial sensitif tidak disimpan di DataStore maupun Room.",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun AccessibleSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
