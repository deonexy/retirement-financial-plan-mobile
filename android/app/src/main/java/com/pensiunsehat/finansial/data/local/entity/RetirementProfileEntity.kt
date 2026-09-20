package com.pensiunsehat.finansial.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "retirement_profiles")
data class RetirementProfileEntity(
    @PrimaryKey val id: Long = 1,
    val currentAge: Int,
    val retirementAge: Int,
    val startingSavingsBalance: String,
    val liquidAssets: String,
    val stockAssets: String,
    val otherInvestmentAssets: String,
    val propertyAssets: String,
    val dplkBalance: String,
    val jhtBalance: String,
    val goldGramsOwned: String,
    val goldGramsPawned: String,
    val goldPawnLiability: String,
    val monthlyRetirementNeeds: String,
    val postRetirementIncomeTarget: String,
    val inflationRatePercent: String,
    val annualGoldGrowthRatePercent: String,
    val targetLegacy: String,
    val notes: String?,
    val updatedAtEpochMs: Long,
)
