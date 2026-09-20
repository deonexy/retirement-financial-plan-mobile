package com.pensiunsehat.finansial.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ProfileUiState(
    val id: Long = 0,
    val currentAge: String = "40",
    val retirementAge: String = "55",
    val startingSavingsBalance: String = "0",
    val liquidAssets: String = "0",
    val stockAssets: String = "0",
    val otherInvestmentAssets: String = "0",
    val propertyAssets: String = "0",
    val dplkBalance: String = "0",
    val jhtBalance: String = "0",
    val goldGramsOwned: String = "0",
    val goldGramsPawned: String = "0",
    val goldPawnLiability: String = "0",
    val monthlyRetirementNeeds: String = "0",
    val postRetirementIncomeTarget: String = "0",
    val inflationRatePercent: String = "0",
    val annualGoldGrowthRatePercent: String = "0",
    val targetLegacy: String = "0",
    val notes: String = "",
    val statusMessage: String? = null,
)

class ProfileViewModel(
    private val repository: LocalFinancialRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        repository.observeProfile().onEach { profile ->
            if (profile != null) {
                _uiState.value = profile.toUiState(statusMessage = _uiState.value.statusMessage)
            }
        }.launchIn(viewModelScope)
    }

    fun update(transform: ProfileUiState.() -> ProfileUiState) {
        _uiState.value = _uiState.value.transform().copy(statusMessage = null)
    }

    fun saveProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveProfile(
                RetirementProfile(
                    id = state.id,
                    currentAge = state.currentAge.toIntOrNull() ?: 0,
                    retirementAge = state.retirementAge.toIntOrNull() ?: 55,
                    startingSavingsBalance = state.startingSavingsBalance.toDecimal(),
                    liquidAssets = state.liquidAssets.toDecimal(),
                    stockAssets = state.stockAssets.toDecimal(),
                    otherInvestmentAssets = state.otherInvestmentAssets.toDecimal(),
                    propertyAssets = state.propertyAssets.toDecimal(),
                    dplkBalance = state.dplkBalance.toDecimal(),
                    jhtBalance = state.jhtBalance.toDecimal(),
                    goldGramsOwned = state.goldGramsOwned.toDecimal(),
                    goldGramsPawned = state.goldGramsPawned.toDecimal(),
                    goldPawnLiability = state.goldPawnLiability.toDecimal(),
                    monthlyRetirementNeeds = state.monthlyRetirementNeeds.toDecimal(),
                    postRetirementIncomeTarget = state.postRetirementIncomeTarget.toDecimal(),
                    inflationRatePercent = state.inflationRatePercent.toDecimal(),
                    annualGoldGrowthRatePercent = state.annualGoldGrowthRatePercent.toDecimal(),
                    targetLegacy = state.targetLegacy.toDecimal(),
                    notes = state.notes.ifBlank { null },
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            _uiState.value = _uiState.value.copy(statusMessage = "Profile tersimpan lokal")
        }
    }

    class Factory(
        private val repository: LocalFinancialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfileViewModel(repository) as T
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Profile pensiun", style = MaterialTheme.typography.headlineSmall) }
        item { FinanceField("Usia sekarang", state.currentAge) { viewModel.update { copy(currentAge = it) } } }
        item { FinanceField("Usia pensiun", state.retirementAge) { viewModel.update { copy(retirementAge = it) } } }
        item { FinanceField("Saldo awal tabungan", state.startingSavingsBalance) { viewModel.update { copy(startingSavingsBalance = it) } } }
        item { FinanceField("Aset likuid", state.liquidAssets) { viewModel.update { copy(liquidAssets = it) } } }
        item { FinanceField("Aset saham", state.stockAssets) { viewModel.update { copy(stockAssets = it) } } }
        item { FinanceField("Investasi lain", state.otherInvestmentAssets) { viewModel.update { copy(otherInvestmentAssets = it) } } }
        item { FinanceField("Aset properti", state.propertyAssets) { viewModel.update { copy(propertyAssets = it) } } }
        item { FinanceField("Saldo DPLK", state.dplkBalance) { viewModel.update { copy(dplkBalance = it) } } }
        item { FinanceField("Saldo JHT", state.jhtBalance) { viewModel.update { copy(jhtBalance = it) } } }
        item { FinanceField("Emas di tangan (gram)", state.goldGramsOwned) { viewModel.update { copy(goldGramsOwned = it) } } }
        item { FinanceField("Emas digadaikan (gram)", state.goldGramsPawned) { viewModel.update { copy(goldGramsPawned = it) } } }
        item { FinanceField("Kewajiban gadai", state.goldPawnLiability) { viewModel.update { copy(goldPawnLiability = it) } } }
        item { FinanceField("Kebutuhan pensiun bulanan", state.monthlyRetirementNeeds) { viewModel.update { copy(monthlyRetirementNeeds = it) } } }
        item { FinanceField("Target pendapatan pasca pensiun", state.postRetirementIncomeTarget) { viewModel.update { copy(postRetirementIncomeTarget = it) } } }
        item { FinanceField("Inflasi tahunan (%)", state.inflationRatePercent) { viewModel.update { copy(inflationRatePercent = it) } } }
        item { FinanceField("Pertumbuhan emas tahunan (%)", state.annualGoldGrowthRatePercent) { viewModel.update { copy(annualGoldGrowthRatePercent = it) } } }
        item { FinanceField("Target warisan", state.targetLegacy) { viewModel.update { copy(targetLegacy = it) } } }
        item {
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.update { copy(notes = it) } },
                label = { Text("Catatan") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(onClick = viewModel::saveProfile, modifier = Modifier.fillMaxWidth()) {
                Text("Simpan profile")
            }
        }
        state.statusMessage?.let { message ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(message, modifier = Modifier.padding(16.dp))
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

private fun RetirementProfile.toUiState(statusMessage: String?) = ProfileUiState(
    id = id,
    currentAge = currentAge.toString(),
    retirementAge = retirementAge.toString(),
    startingSavingsBalance = startingSavingsBalance.toPlainString(),
    liquidAssets = liquidAssets.toPlainString(),
    stockAssets = stockAssets.toPlainString(),
    otherInvestmentAssets = otherInvestmentAssets.toPlainString(),
    propertyAssets = propertyAssets.toPlainString(),
    dplkBalance = dplkBalance.toPlainString(),
    jhtBalance = jhtBalance.toPlainString(),
    goldGramsOwned = goldGramsOwned.toPlainString(),
    goldGramsPawned = goldGramsPawned.toPlainString(),
    goldPawnLiability = goldPawnLiability.toPlainString(),
    monthlyRetirementNeeds = monthlyRetirementNeeds.toPlainString(),
    postRetirementIncomeTarget = postRetirementIncomeTarget.toPlainString(),
    inflationRatePercent = inflationRatePercent.toPlainString(),
    annualGoldGrowthRatePercent = annualGoldGrowthRatePercent.toPlainString(),
    targetLegacy = targetLegacy.toPlainString(),
    notes = notes.orEmpty(),
    statusMessage = statusMessage,
)

private fun String.toDecimal(): BigDecimal = toBigDecimalOrNull() ?: BigDecimal.ZERO
