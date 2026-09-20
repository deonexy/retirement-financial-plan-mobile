package com.pensiunsehat.finansial.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pensiunsehat.finansial.data.repository.LocalFinancialRepository
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import com.pensiunsehat.finansial.data.repository.ThemePreference
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.GoldPriceStatus
import com.pensiunsehat.finansial.worker.BackgroundWorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val financialRepository: LocalFinancialRepository,
    private val backgroundWorkScheduler: BackgroundWorkScheduler,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        financialRepository.observeGoldPriceSnapshot(),
    ) { settings, goldPrice ->
        SettingsUiState(
            themePreference = settings.themePreference,
            monthlyReminderEnabled = settings.monthlyReminderEnabled,
            reminderDayOfMonth = settings.reminderDayOfMonth,
            goldPriceAutoRefresh = settings.goldPriceAutoRefresh,
            lastGoldStatus = goldPrice?.capturedAtIso?.let { "Harga terakhir tersimpan: $it" } ?: "Belum ada snapshot harga emas",
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
            backgroundWorkScheduler.setGoldPriceRefreshEnabled(enabled)
        }
    }

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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepository, financialRepository, backgroundWorkScheduler) as T
    }
}

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val monthlyReminderEnabled: Boolean = false,
    val reminderDayOfMonth: Int = 1,
    val goldPriceAutoRefresh: Boolean = false,
    val lastGoldStatus: String = "Belum ada snapshot harga emas",
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tema")
                ThemePreference.entries.forEach { theme ->
                    Button(onClick = { viewModel.setThemePreference(theme) }, modifier = Modifier.fillMaxWidth()) {
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
                Text("Hari pengingat: ${state.reminderDayOfMonth}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.setReminderDayOfMonth(state.reminderDayOfMonth - 1) }) {
                        Text("-1 hari")
                    }
                    Button(onClick = { viewModel.setReminderDayOfMonth(state.reminderDayOfMonth + 1) }) {
                        Text("+1 hari")
                    }
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccessibleSwitchRow(
                    label = "Refresh harga emas saat jaringan tersedia",
                    checked = state.goldPriceAutoRefresh,
                    onCheckedChange = viewModel::setGoldPriceAutoRefresh,
                )
                Text(state.lastGoldStatus)
                Button(onClick = viewModel::seedOfflineGoldSnapshot, modifier = Modifier.fillMaxWidth()) {
                    Text("Simpan snapshot harga contoh")
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
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
