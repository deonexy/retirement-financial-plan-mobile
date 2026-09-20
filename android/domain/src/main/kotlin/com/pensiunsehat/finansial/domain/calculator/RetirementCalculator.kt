package com.pensiunsehat.finansial.domain.calculator

import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.AssetType
import com.pensiunsehat.finansial.domain.model.FinancialSummary
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import java.math.BigDecimal

object RetirementCalculator {
    fun calculateSummary(
        profile: RetirementProfile,
        updates: List<FinancialUpdate>,
        purchases: List<AssetPurchase>,
        goldPriceSnapshot: GoldPriceSnapshot?,
    ): FinancialSummary {
        val currentSavings = SavingsCalculator.calculateCurrentSavings(profile, updates, purchases)
        val accumulatedSurplus = SavingsCalculator.calculateAccumulatedSurplus(updates)
        val latestSurplus = updates.maxByOrNull { it.updateDate }?.let(SavingsCalculator::calculatePositiveSurplus) ?: BigDecimal.ZERO
        val purchasesFromSavings = SavingsCalculator.calculatePurchasesFromSavings(purchases)
        val goldValuation = GoldCalculator.calculate(profile, goldPriceSnapshot)
        val purchasedStocks = purchases.filter { it.assetType == AssetType.STOCK }.fold(BigDecimal.ZERO) { total, purchase -> total + purchase.purchaseValueRupiah }
        val purchasedOtherInvestments = purchases.filter { it.assetType == AssetType.OTHER_INVESTMENT }.fold(BigDecimal.ZERO) { total, purchase -> total + purchase.purchaseValueRupiah }
        val purchasedProperty = purchases.filter { it.assetType == AssetType.PROPERTY }.fold(BigDecimal.ZERO) { total, purchase -> total + purchase.purchaseValueRupiah }
        val retirementAssets = profile.dplkBalance + profile.jhtBalance
        val baseAssetTotal = currentSavings +
            profile.liquidAssets +
            profile.stockAssets + purchasedStocks +
            profile.otherInvestmentAssets + purchasedOtherInvestments +
            profile.propertyAssets + purchasedProperty +
            retirementAssets
        val totalNetAssets = goldValuation.netValue?.let { baseAssetTotal + it }
        val suggestions = buildList {
            if (updates.isEmpty()) add("Tambahkan update bulanan agar surplus terbaru dapat dihitung.")
            if (purchasesFromSavings > currentSavings) add("Tinjau pembelian dari tabungan agar saldo tidak tergerus terlalu dalam.")
            if (goldValuation.netValue == null) add("Simpan snapshot harga emas agar total aset bersih lebih lengkap saat offline.")
            if (latestSurplus == BigDecimal.ZERO && updates.isNotEmpty()) add("Surplus bulan terakhir nol; cek pos gaya hidup, kesehatan, dan cicilan.")
        }

        return FinancialSummary(
            currentSavings = currentSavings,
            latestSurplus = latestSurplus,
            accumulatedSurplus = accumulatedSurplus,
            purchasesFromSavings = purchasesFromSavings,
            goldSaleValue = goldValuation.totalSaleValue,
            netGoldValue = goldValuation.netValue,
            retirementAssets = retirementAssets,
            totalNetAssets = totalNetAssets,
            goldPriceStatusLabel = goldValuation.statusLabel,
            suggestions = suggestions,
        )
    }
}
