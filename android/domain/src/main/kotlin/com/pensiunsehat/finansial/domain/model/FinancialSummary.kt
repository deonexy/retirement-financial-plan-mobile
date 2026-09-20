package com.pensiunsehat.finansial.domain.model

import java.math.BigDecimal

data class FinancialSummary(
    val currentSavings: BigDecimal = BigDecimal.ZERO,
    val latestSurplus: BigDecimal = BigDecimal.ZERO,
    val accumulatedSurplus: BigDecimal = BigDecimal.ZERO,
    val purchasesFromSavings: BigDecimal = BigDecimal.ZERO,
    val goldSaleValue: BigDecimal? = null,
    val netGoldValue: BigDecimal? = null,
    val retirementAssets: BigDecimal = BigDecimal.ZERO,
    val totalNetAssets: BigDecimal? = null,
    val goldPriceStatusLabel: String = "Harga emas belum tersedia",
    val suggestions: List<String> = emptyList(),
) {
    companion object {
        val Empty = FinancialSummary()
    }
}
