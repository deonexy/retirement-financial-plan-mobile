package com.pensiunsehat.finansial.domain.calculator

import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.AssetType
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import com.pensiunsehat.finansial.domain.model.FundingSource
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.GoldPriceStatus
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetirementCalculatorTest {
    private val baseProfile = RetirementProfile(
        startingSavingsBalance = bd("1000000"),
        liquidAssets = bd("500000"),
        stockAssets = bd("750000"),
        otherInvestmentAssets = bd("250000"),
        propertyAssets = bd("2000000"),
        dplkBalance = bd("300000"),
        jhtBalance = bd("400000"),
        goldGramsOwned = bd("5"),
        goldGramsPawned = bd("2"),
        goldPawnLiability = bd("1000000"),
    )

    @Test
    fun positiveSurplusIsAdded() {
        val update = update(
            netIncome = "10000000",
            mandatory = "3000000",
            lifestyle = "1000000",
            health = "500000",
            debt = "2000000",
        )

        assertMoney("3500000", SavingsCalculator.calculatePositiveSurplus(update))
    }

    @Test
    fun negativeSurplusDoesNotReduceSavingsAutomatically() {
        val update = update(
            netIncome = "3000000",
            mandatory = "2000000",
            lifestyle = "1500000",
            health = "500000",
            debt = "500000",
        )

        val savings = SavingsCalculator.calculateCurrentSavings(baseProfile, listOf(update), emptyList())

        assertMoney("1000000", savings)
    }

    @Test
    fun purchaseFromSavingsReducesSavingsBalance() {
        val purchase = purchase(value = "250000", fundingSource = FundingSource.SAVINGS)

        val savings = SavingsCalculator.calculateCurrentSavings(baseProfile, emptyList(), listOf(purchase))

        assertMoney("750000", savings)
    }

    @Test
    fun purchaseFromNonSavingsDoesNotReduceSavingsBalance() {
        val purchase = purchase(value = "250000", fundingSource = FundingSource.NON_SAVINGS)

        val savings = SavingsCalculator.calculateCurrentSavings(baseProfile, emptyList(), listOf(purchase))

        assertMoney("1000000", savings)
    }

    @Test
    fun pawnedGoldUsesAllGramsAndSubtractsPawnLiability() {
        val valuation = GoldCalculator.calculate(
            profile = baseProfile,
            snapshot = GoldPriceSnapshot(
                pricePerGram = bd("1500000"),
                capturedAtIso = "2026-09-20",
                status = GoldPriceStatus.LIVE,
            ),
        )

        assertMoney("10500000", valuation.totalSaleValue!!)
        assertMoney("9500000", valuation.netValue!!)
    }

    @Test
    fun missingGoldPriceKeepsStatusExplicit() {
        val valuation = GoldCalculator.calculate(baseProfile, snapshot = null)

        assertNull(valuation.totalSaleValue)
        assertEquals("Harga emas belum tersedia", valuation.statusLabel)
    }

    @Test
    fun lastStoredGoldPriceIsLabeledWhenOffline() {
        val valuation = GoldCalculator.calculate(
            profile = baseProfile,
            snapshot = GoldPriceSnapshot(
                pricePerGram = bd("1400000"),
                capturedAtIso = "2026-09-19",
                status = GoldPriceStatus.STALE,
            ),
        )

        assertEquals("Harga terakhir diperbarui pada 2026-09-19 (stale)", valuation.statusLabel)
        assertMoney("8800000", valuation.netValue!!)
    }

    @Test
    fun totalAssetsIncludeSavingsGoldAndOtherAssetBuckets() {
        val summary = RetirementCalculator.calculateSummary(
            profile = baseProfile,
            updates = listOf(
                update(
                    netIncome = "8000000",
                    mandatory = "2000000",
                    lifestyle = "1000000",
                    health = "500000",
                    debt = "500000",
                ),
            ),
            purchases = listOf(
                purchase(assetType = AssetType.STOCK, value = "100000", fundingSource = FundingSource.NON_SAVINGS),
            ),
            goldPriceSnapshot = GoldPriceSnapshot(
                pricePerGram = bd("1500000"),
                capturedAtIso = "2026-09-20",
                status = GoldPriceStatus.LIVE,
            ),
        )

        assertMoney("18800000", summary.totalNetAssets!!)
    }

    private fun update(
        netIncome: String,
        mandatory: String,
        lifestyle: String,
        health: String,
        debt: String,
    ) = FinancialUpdate(
        updateDate = "2026-09-01",
        netIncome = bd(netIncome),
        mandatoryExpenses = bd(mandatory),
        lifestyleExpenses = bd(lifestyle),
        healthExpenses = bd(health),
        debtPayments = bd(debt),
    )

    private fun purchase(
        assetType: AssetType = AssetType.OTHER_INVESTMENT,
        value: String,
        fundingSource: FundingSource,
    ) = AssetPurchase(
        assetType = assetType,
        assetName = "Aset",
        quantity = bd("1"),
        unit = "unit",
        purchasePriceRupiah = bd(value),
        purchaseValueRupiah = bd(value),
        purchaseDate = "2026-09-01",
        fundingSource = fundingSource,
    )

    private fun bd(value: String) = BigDecimal(value)

    private fun assertMoney(expected: String, actual: BigDecimal) {
        assertEquals(0, actual.compareTo(BigDecimal(expected)))
    }
}
