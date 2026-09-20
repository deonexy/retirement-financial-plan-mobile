package com.pensiunsehat.finansial.domain.model

import java.math.BigDecimal

data class FinancialUpdate(
    val id: Long = 0,
    val updateDate: String,
    val netIncome: BigDecimal,
    val mandatoryExpenses: BigDecimal,
    val lifestyleExpenses: BigDecimal,
    val healthExpenses: BigDecimal,
    val debtPayments: BigDecimal,
    val remainingDebt: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val updatedAtEpochMs: Long = 0,
)
