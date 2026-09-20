package com.pensiunsehat.finansial.data.repository

import com.pensiunsehat.finansial.data.local.dao.AssetPurchaseDao
import com.pensiunsehat.finansial.data.local.dao.FinancialUpdateDao
import com.pensiunsehat.finansial.data.local.dao.GoldPriceSnapshotDao
import com.pensiunsehat.finansial.data.local.dao.RetirementProfileDao
import com.pensiunsehat.finansial.data.local.entity.AssetPurchaseEntity
import com.pensiunsehat.finansial.data.local.entity.FinancialUpdateEntity
import com.pensiunsehat.finansial.data.local.entity.GoldPriceSnapshotEntity
import com.pensiunsehat.finansial.data.local.entity.RetirementProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomLocalFinancialRepositoryTest {
    @Test
    fun fakeDaoOrderingKeepsMixedDateFormatsChronological() = runTest {
        val updateDao = FakeFinancialUpdateDao()

        updateDao.upsert(
            FinancialUpdateEntity(
                updateDate = "2026-9-30",
                netIncome = "0",
                mandatoryExpenses = "0",
                lifestyleExpenses = "0",
                healthExpenses = "0",
                debtPayments = "0",
                remainingDebt = "0",
                notes = null,
                updatedAtEpochMs = 1,
            ),
        )
        updateDao.upsert(
            FinancialUpdateEntity(
                updateDate = "2026-10-01",
                netIncome = "0",
                mandatoryExpenses = "0",
                lifestyleExpenses = "0",
                healthExpenses = "0",
                debtPayments = "0",
                remainingDebt = "0",
                notes = null,
                updatedAtEpochMs = 2,
            ),
        )

        assertEquals("2026-10-01", updateDao.observeAll().first().first().updateDate)
    }

    @Test
    fun observeFinancialSummaryCombinesLocalFlows() = runTest {
        val profileDao = FakeRetirementProfileDao()
        val updateDao = FakeFinancialUpdateDao()
        val purchaseDao = FakeAssetPurchaseDao()
        val goldDao = FakeGoldPriceSnapshotDao()
        val repository = RoomLocalFinancialRepository(profileDao, updateDao, purchaseDao, goldDao)

        repository.saveProfile(
            RetirementProfileEntity(
                currentAge = 40,
                retirementAge = 55,
                startingSavingsBalance = "1000000",
                liquidAssets = "500000",
                stockAssets = "0",
                otherInvestmentAssets = "0",
                propertyAssets = "0",
                dplkBalance = "0",
                jhtBalance = "0",
                goldGramsOwned = "2",
                goldGramsPawned = "1",
                goldPawnLiability = "1000000",
                monthlyRetirementNeeds = "0",
                postRetirementIncomeTarget = "0",
                inflationRatePercent = "0",
                annualGoldGrowthRatePercent = "0",
                targetLegacy = "0",
                notes = null,
                updatedAtEpochMs = 1,
            ).toDomain(),
        )
        repository.saveFinancialUpdate(
            FinancialUpdateEntity(
                updateDate = "2026-09-01",
                netIncome = "6000000",
                mandatoryExpenses = "1000000",
                lifestyleExpenses = "500000",
                healthExpenses = "500000",
                debtPayments = "500000",
                remainingDebt = "0",
                notes = null,
                updatedAtEpochMs = 1,
            ).toDomain(),
        )
        repository.saveAssetPurchase(
            AssetPurchaseEntity(
                assetType = "OTHER_INVESTMENT",
                assetName = "Reksa Dana",
                quantity = "1",
                unit = "unit",
                purchasePriceRupiah = "250000",
                purchaseValueRupiah = "250000",
                purchaseDate = "2026-09-10",
                fundingSource = "SAVINGS",
                notes = null,
                updatedAtEpochMs = 1,
            ).toDomain(),
        )
        repository.saveGoldPriceSnapshot(
            GoldPriceSnapshotEntity(
                pricePerGram = "1500000",
                currency = "IDR",
                source = "test",
                capturedAtIso = "2026-09-20",
                status = "STALE",
            ).toDomain(),
        )

        val summary = repository.observeFinancialSummary().first()

        assertEquals("4250000", summary.currentSavings.stripTrailingZeros().toPlainString())
        assertEquals("3500000", summary.accumulatedSurplus.stripTrailingZeros().toPlainString())
        assertEquals("3500000", summary.netGoldValue?.stripTrailingZeros()?.toPlainString())
        assertEquals("Harga terakhir diperbarui pada 2026-09-20 (stale)", summary.goldPriceStatusLabel)
    }

    private class FakeRetirementProfileDao : RetirementProfileDao {
        private val state = MutableStateFlow<RetirementProfileEntity?>(null)
        override fun observeLatest(): Flow<RetirementProfileEntity?> = state
        override suspend fun upsert(entity: RetirementProfileEntity) {
            state.value = entity
        }
    }

    private class FakeFinancialUpdateDao : FinancialUpdateDao {
        private val state = MutableStateFlow<List<FinancialUpdateEntity>>(emptyList())
        override fun observeAll(): Flow<List<FinancialUpdateEntity>> = state
        override suspend fun upsert(entity: FinancialUpdateEntity) {
            state.value = (state.value + entity).sortedByDescending { flexibleEpochDay(it.updateDate) }
        }
    }

    private class FakeAssetPurchaseDao : AssetPurchaseDao {
        private val state = MutableStateFlow<List<AssetPurchaseEntity>>(emptyList())
        override fun observeAll(): Flow<List<AssetPurchaseEntity>> = state
        override suspend fun upsert(entity: AssetPurchaseEntity) {
            state.value = (state.value + entity).sortedByDescending { flexibleEpochDay(it.purchaseDate) }
        }
    }

    private class FakeGoldPriceSnapshotDao : GoldPriceSnapshotDao {
        private val state = MutableStateFlow<GoldPriceSnapshotEntity?>(null)
        override fun observeLatest(): Flow<GoldPriceSnapshotEntity?> = state
        override suspend fun upsert(entity: GoldPriceSnapshotEntity) {
            state.value = entity
        }
    }
}

private fun flexibleEpochDay(value: String): Long {
    val parts = value.split('-')
    if (parts.size != 3) return 0L
    val year = parts[0].toIntOrNull() ?: return 0L
    val month = parts[1].toIntOrNull() ?: return 0L
    val day = parts[2].toIntOrNull() ?: return 0L
    return runCatching { java.time.LocalDate.of(year, month, day).toEpochDay() }.getOrDefault(0L)
}
