package com.pensiunsehat.finansial.presentation.monthlyupdate

import com.pensiunsehat.finansial.data.repository.LocalFinancialRepository
import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.FinancialSummary
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyUpdateViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun savePersistsUpdateAndResetsEditableFields() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeFinancialRepository()
            val viewModel = MonthlyUpdateViewModel(repository)

            viewModel.update {
                copy(
                    updateDate = "2026-09-01",
                    netIncome = "7000000",
                    mandatoryExpenses = "1000000",
                    lifestyleExpenses = "500000",
                    healthExpenses = "250000",
                    debtPayments = "250000",
                    remainingDebt = "4000000",
                    notes = "bulan pertama",
                )
            }

            viewModel.save()
            advanceUntilIdle()

            val saved = repository.updates.value.single()
            assertEquals("7000000", saved.netIncome.stripTrailingZeros().toPlainString())
            assertEquals("0", viewModel.uiState.value.netIncome)
            assertEquals("0", viewModel.uiState.value.remainingDebt)
            assertEquals("", viewModel.uiState.value.notes)
            assertEquals("Update bulanan tersimpan lokal", viewModel.uiState.value.statusMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeFinancialRepository : LocalFinancialRepository {
        private val profile = MutableStateFlow<RetirementProfile?>(null)
        val updates = MutableStateFlow<List<FinancialUpdate>>(emptyList())
        private val purchases = MutableStateFlow<List<AssetPurchase>>(emptyList())
        private val gold = MutableStateFlow<GoldPriceSnapshot?>(null)

        override fun observeProfile(): Flow<RetirementProfile?> = profile.asStateFlow()
        override fun observeFinancialUpdates(): Flow<List<FinancialUpdate>> = updates.asStateFlow()
        override fun observeAssetPurchases(): Flow<List<AssetPurchase>> = purchases.asStateFlow()
        override fun observeGoldPriceSnapshot(): Flow<GoldPriceSnapshot?> = gold.asStateFlow()
        override fun observeFinancialSummary(): Flow<FinancialSummary> = flowOf(FinancialSummary.Empty)
        override suspend fun saveProfile(profile: RetirementProfile) {
            this.profile.value = profile
        }
        override suspend fun saveFinancialUpdate(update: FinancialUpdate) {
            updates.value = updates.value + update
        }
        override suspend fun saveAssetPurchase(purchase: AssetPurchase) {
            purchases.value = purchases.value + purchase
        }
        override suspend fun saveGoldPriceSnapshot(snapshot: GoldPriceSnapshot) {
            gold.value = snapshot
        }
    }
}
