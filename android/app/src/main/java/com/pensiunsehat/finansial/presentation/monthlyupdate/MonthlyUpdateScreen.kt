package com.pensiunsehat.finansial.presentation.monthlyupdate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pensiunsehat.finansial.data.repository.LocalFinancialRepository
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class MonthlyUpdateUiState(
    val updateDate: String = "2026-09-01",
    val netIncome: String = "0",
    val mandatoryExpenses: String = "0",
    val lifestyleExpenses: String = "0",
    val healthExpenses: String = "0",
    val debtPayments: String = "0",
    val remainingDebt: String = "0",
    val notes: String = "",
    val history: List<FinancialUpdate> = emptyList(),
    val statusMessage: String? = null,
)

class MonthlyUpdateViewModel(
    private val repository: LocalFinancialRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MonthlyUpdateUiState())
    val uiState: StateFlow<MonthlyUpdateUiState> = _uiState.asStateFlow()

    init {
        repository.observeFinancialUpdates().onEach { history ->
            _uiState.value = _uiState.value.copy(history = history)
        }.launchIn(viewModelScope)
    }

    fun update(transform: MonthlyUpdateUiState.() -> MonthlyUpdateUiState) {
        _uiState.value = _uiState.value.transform().copy(statusMessage = null)
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveFinancialUpdate(
                FinancialUpdate(
                    updateDate = state.updateDate,
                    netIncome = state.netIncome.toDecimal(),
                    mandatoryExpenses = state.mandatoryExpenses.toDecimal(),
                    lifestyleExpenses = state.lifestyleExpenses.toDecimal(),
                    healthExpenses = state.healthExpenses.toDecimal(),
                    debtPayments = state.debtPayments.toDecimal(),
                    remainingDebt = state.remainingDebt.toDecimal(),
                    notes = state.notes.ifBlank { null },
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            _uiState.value = state.copy(
                netIncome = "0",
                mandatoryExpenses = "0",
                lifestyleExpenses = "0",
                healthExpenses = "0",
                debtPayments = "0",
                remainingDebt = "0",
                notes = "",
                statusMessage = "Update bulanan tersimpan lokal",
            )
        }
    }

    class Factory(
        private val repository: LocalFinancialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MonthlyUpdateViewModel(repository) as T
    }
}

@Composable
fun MonthlyUpdateScreen(viewModel: MonthlyUpdateViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Update bulanan", style = MaterialTheme.typography.headlineSmall) }
        item {
            FinanceField("Tanggal update (yyyy-MM-dd)", state.updateDate) { viewModel.update { copy(updateDate = it) } }
        }
        item { FinanceField("Pendapatan bersih", state.netIncome) { viewModel.update { copy(netIncome = it) } } }
        item { FinanceField("Kebutuhan wajib", state.mandatoryExpenses) { viewModel.update { copy(mandatoryExpenses = it) } } }
        item { FinanceField("Gaya hidup", state.lifestyleExpenses) { viewModel.update { copy(lifestyleExpenses = it) } } }
        item { FinanceField("Pengeluaran kesehatan", state.healthExpenses) { viewModel.update { copy(healthExpenses = it) } } }
        item { FinanceField("Pembayaran utang", state.debtPayments) { viewModel.update { copy(debtPayments = it) } } }
        item { FinanceField("Sisa utang", state.remainingDebt) { viewModel.update { copy(remainingDebt = it) } } }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { copy(notes = it) } },
                label = { Text("Catatan") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text("Simpan update")
            }
        }
        state.statusMessage?.let { message ->
            item { Card(modifier = Modifier.fillMaxWidth()) { Text(message, modifier = Modifier.padding(16.dp)) } }
        }
        if (state.history.isNotEmpty()) {
            item { Text("Riwayat lokal", style = MaterialTheme.typography.titleMedium) }
            items(state.history, key = { it.id }) { update ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(update.updateDate, style = MaterialTheme.typography.labelLarge)
                        Text("Pendapatan: Rp ${update.netIncome.toPlainString()}")
                        Text("Sisa utang: Rp ${update.remainingDebt.toPlainString()}")
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

private fun String.toDecimal(): BigDecimal = toBigDecimalOrNull() ?: BigDecimal.ZERO
