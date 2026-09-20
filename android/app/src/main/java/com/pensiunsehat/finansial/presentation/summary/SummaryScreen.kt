package com.pensiunsehat.finansial.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pensiunsehat.finansial.domain.model.FinancialSummary
import com.pensiunsehat.finansial.domain.usecase.ObserveFinancialSummaryUseCase
import java.math.BigDecimal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SummaryViewModel(
    observeFinancialSummaryUseCase: ObserveFinancialSummaryUseCase,
) : ViewModel() {
    val uiState: StateFlow<FinancialSummary> = observeFinancialSummaryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinancialSummary.Empty)

    class Factory(
        private val observeFinancialSummaryUseCase: ObserveFinancialSummaryUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SummaryViewModel(observeFinancialSummaryUseCase) as T
    }
}

@Composable
fun SummaryScreen(viewModel: SummaryViewModel) {
    val summary by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ringkasan", style = MaterialTheme.typography.headlineSmall)
        SummaryCard("Saldo tabungan saat ini", summary.currentSavings)
        SummaryCard("Surplus bulan terakhir", summary.latestSurplus)
        SummaryCard("Akumulasi surplus", summary.accumulatedSurplus)
        SummaryCard("Pembelian dari tabungan", summary.purchasesFromSavings)
        SummaryCard("Aset pensiun DPLK/JHT", summary.retirementAssets)
        SummaryCard("Nilai bersih emas", summary.netGoldValue)
        SummaryCard("Total aset bersih", summary.totalNetAssets)
        StatusCard("Status harga emas", summary.goldPriceStatusLabel)
        if (summary.suggestions.isNotEmpty()) {
            StatusCard(
                title = "Saran tindakan",
                value = summary.suggestions.joinToString(separator = "
• ", prefix = "• "),
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, amount: BigDecimal?) {
    StatusCard(title = title, value = amount?.toRupiah() ?: "Menunggu data")
}

@Composable
private fun StatusCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun BigDecimal.toRupiah(): String = "Rp ${stripTrailingZeros().toPlainString()}"
