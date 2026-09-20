package com.pensiunsehat.finansial.domain.calculator

import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import com.pensiunsehat.finansial.domain.model.FundingSource
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import java.math.BigDecimal

object SavingsCalculator {
    fun calculatePositiveSurplus(update: FinancialUpdate): BigDecimal {
        val rawSurplus = update.netIncome
            .subtract(update.mandatoryExpenses)
            .subtract(update.lifestyleExpenses)
            .subtract(update.healthExpenses)
            .subtract(update.debtPayments)
        return rawSurplus.takeIf { it >= BigDecimal.ZERO } ?: BigDecimal.ZERO
    }

    fun calculateAccumulatedSurplus(updates: List<FinancialUpdate>): BigDecimal =
        updates.fold(BigDecimal.ZERO) { total, update -> total + calculatePositiveSurplus(update) }

    fun calculatePurchasesFromSavings(purchases: List<AssetPurchase>): BigDecimal =
        purchases
            .filter { it.fundingSource == FundingSource.SAVINGS }
            .fold(BigDecimal.ZERO) { total, purchase -> total + purchase.purchaseValueRupiah }

    fun calculateCurrentSavings(
        profile: RetirementProfile,
        updates: List<FinancialUpdate>,
        purchases: List<AssetPurchase>,
    ): BigDecimal =
        profile.startingSavingsBalance +
            calculateAccumulatedSurplus(updates) -
            calculatePurchasesFromSavings(purchases)
}
