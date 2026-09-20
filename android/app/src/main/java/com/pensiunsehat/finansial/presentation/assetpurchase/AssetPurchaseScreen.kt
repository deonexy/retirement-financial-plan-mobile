package com.pensiunsehat.finansial.presentation.assetpurchase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.AssetType
import com.pensiunsehat.finansial.domain.model.FundingSource
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class AssetPurchaseUiState(
    val assetType: AssetType = AssetType.GOLD,
    val fundingSource: FundingSource = FundingSource.SAVINGS,
    val assetName: String = "",
    val quantity: String = "1",
    val unit: String = "gram",
    val purchasePriceRupiah: String = "0",
    val purchaseValueRupiah: String = "0",
    val purchaseDate: String = "2026-09-01",
    val notes: String = "",
    val history: List<AssetPurchase> = emptyList(),
    val statusMessage: String? = null,
)

class AssetPurchaseViewModel(
    private val repository: LocalFinancialRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssetPurchaseUiState())
    val uiState: StateFlow<AssetPurchaseUiState> = _uiState.asStateFlow()

    init {
        repository.observeAssetPurchases().onEach { history ->
            _uiState.value = _uiState.value.copy(history = history)
        }.launchIn(viewModelScope)
    }

    fun update(transform: AssetPurchaseUiState.() -> AssetPurchaseUiState) {
        _uiState.value = _uiState.value.transform().copy(statusMessage = null)
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveAssetPurchase(
                AssetPurchase(
                    assetType = state.assetType,
                    assetName = state.assetName.ifBlank { state.assetType.name },
                    quantity = state.quantity.toDecimal(),
                    unit = state.unit,
                    purchasePriceRupiah = state.purchasePriceRupiah.toDecimal(),
                    purchaseValueRupiah = state.purchaseValueRupiah.toDecimal(),
                    purchaseDate = state.purchaseDate,
                    fundingSource = state.fundingSource,
                    notes = state.notes.ifBlank { null },
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            _uiState.value = AssetPurchaseUiState(
                history = _uiState.value.history,
                statusMessage = "Pembelian aset tersimpan lokal",
            )
        }
    }

    class Factory(
        private val repository: LocalFinancialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AssetPurchaseViewModel(repository) as T
    }
}

@Composable
fun AssetPurchaseScreen(viewModel: AssetPurchaseViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Pembelian aset", style = MaterialTheme.typography.headlineSmall) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetType.entries.forEach { type ->
                    Button(onClick = { viewModel.update { copy(assetType = type) } }) { Text(type.name) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FundingSource.entries.forEach { source ->
                    Button(onClick = { viewModel.update { copy(fundingSource = source) } }) { Text(source.name) }
                }
            }
        }
        item { FinanceField("Nama aset", state.assetName) { viewModel.update { copy(assetName = it) } } }
        item { FinanceField("Jumlah", state.quantity) { viewModel.update { copy(quantity = it) } } }
        item { FinanceField("Satuan", state.unit) { viewModel.update { copy(unit = it) } } }
        item { FinanceField("Harga beli", state.purchasePriceRupiah) { viewModel.update { copy(purchasePriceRupiah = it) } } }
        item { FinanceField("Nilai pembelian", state.purchaseValueRupiah) { viewModel.update { copy(purchaseValueRupiah = it) } } }
        item { FinanceField("Tanggal pembelian (yyyy-MM-dd)", state.purchaseDate) { viewModel.update { copy(purchaseDate = it) } } }
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
                Text("Simpan pembelian")
            }
        }
        state.statusMessage?.let { message ->
            item { Card(modifier = Modifier.fillMaxWidth()) { Text(message, modifier = Modifier.padding(16.dp)) } }
        }
        if (state.history.isNotEmpty()) {
            item { Text("Riwayat pembelian", style = MaterialTheme.typography.titleMedium) }
            items(state.history, key = { it.id }) { purchase ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${purchase.assetType.name} • ${purchase.assetName}", style = MaterialTheme.typography.labelLarge)
                        Text("${purchase.purchaseDate} • ${purchase.fundingSource.name}")
                        Text("Nilai: Rp ${purchase.purchaseValueRupiah.toPlainString()}")
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
