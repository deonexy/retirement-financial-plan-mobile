package com.pensiunsehat.finansial.domain.model

import java.math.BigDecimal

data class RetirementProfile(
    val id: Long = 0,
    val currentAge: Int = 0,
    val retirementAge: Int = 55,
    val startingSavingsBalance: BigDecimal = BigDecimal.ZERO,
    val liquidAssets: BigDecimal = BigDecimal.ZERO,
    val stockAssets: BigDecimal = BigDecimal.ZERO,
    val otherInvestmentAssets: BigDecimal = BigDecimal.ZERO,
    val propertyAssets: BigDecimal = BigDecimal.ZERO,
    val dplkBalance: BigDecimal = BigDecimal.ZERO,
    val jhtBalance: BigDecimal = BigDecimal.ZERO,
    val goldGramsOwned: BigDecimal = BigDecimal.ZERO,
    val goldGramsPawned: BigDecimal = BigDecimal.ZERO,
    val goldPawnLiability: BigDecimal = BigDecimal.ZERO,
    val monthlyRetirementNeeds: BigDecimal = BigDecimal.ZERO,
    val postRetirementIncomeTarget: BigDecimal = BigDecimal.ZERO,
    val inflationRatePercent: BigDecimal = BigDecimal.ZERO,
    val annualGoldGrowthRatePercent: BigDecimal = BigDecimal.ZERO,
    val targetLegacy: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val updatedAtEpochMs: Long = 0,
)
