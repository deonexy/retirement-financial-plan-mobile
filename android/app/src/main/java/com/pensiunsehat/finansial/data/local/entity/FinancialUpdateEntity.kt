package com.pensiunsehat.finansial.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_updates")
data class FinancialUpdateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val updateDate: String,
    val sortDateEpochDay: Long = 0,
    val netIncome: String,
    val mandatoryExpenses: String,
    val lifestyleExpenses: String,
    val healthExpenses: String,
    val debtPayments: String,
    val remainingDebt: String,
    val notes: String?,
    val updatedAtEpochMs: Long,
)
