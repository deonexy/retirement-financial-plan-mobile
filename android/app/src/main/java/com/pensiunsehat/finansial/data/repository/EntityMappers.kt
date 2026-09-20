package com.pensiunsehat.finansial.data.repository

import com.pensiunsehat.finansial.data.local.entity.AssetPurchaseEntity
import com.pensiunsehat.finansial.data.local.entity.FinancialUpdateEntity
import com.pensiunsehat.finansial.data.local.entity.GoldPriceSnapshotEntity
import com.pensiunsehat.finansial.data.local.entity.RetirementProfileEntity
import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.AssetType
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import com.pensiunsehat.finansial.domain.model.FundingSource
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.GoldPriceStatus
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import java.math.BigDecimal

fun RetirementProfileEntity.toDomain() = RetirementProfile(
    id = id,
    currentAge = currentAge,
    retirementAge = retirementAge,
    startingSavingsBalance = startingSavingsBalance.toBigDecimalSafe(),
    liquidAssets = liquidAssets.toBigDecimalSafe(),
    stockAssets = stockAssets.toBigDecimalSafe(),
    otherInvestmentAssets = otherInvestmentAssets.toBigDecimalSafe(),
    propertyAssets = propertyAssets.toBigDecimalSafe(),
    dplkBalance = dplkBalance.toBigDecimalSafe(),
    jhtBalance = jhtBalance.toBigDecimalSafe(),
    goldGramsOwned = goldGramsOwned.toBigDecimalSafe(),
    goldGramsPawned = goldGramsPawned.toBigDecimalSafe(),
    goldPawnLiability = goldPawnLiability.toBigDecimalSafe(),
    monthlyRetirementNeeds = monthlyRetirementNeeds.toBigDecimalSafe(),
    postRetirementIncomeTarget = postRetirementIncomeTarget.toBigDecimalSafe(),
    inflationRatePercent = inflationRatePercent.toBigDecimalSafe(),
    annualGoldGrowthRatePercent = annualGoldGrowthRatePercent.toBigDecimalSafe(),
    targetLegacy = targetLegacy.toBigDecimalSafe(),
    notes = notes,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun RetirementProfile.toEntity() = RetirementProfileEntity(
    id = if (id == 0L) 1L else id,
    currentAge = currentAge,
    retirementAge = retirementAge,
    startingSavingsBalance = startingSavingsBalance.toPlainString(),
    liquidAssets = liquidAssets.toPlainString(),
    stockAssets = stockAssets.toPlainString(),
    otherInvestmentAssets = otherInvestmentAssets.toPlainString(),
    propertyAssets = propertyAssets.toPlainString(),
    dplkBalance = dplkBalance.toPlainString(),
    jhtBalance = jhtBalance.toPlainString(),
    goldGramsOwned = goldGramsOwned.toPlainString(),
    goldGramsPawned = goldGramsPawned.toPlainString(),
    goldPawnLiability = goldPawnLiability.toPlainString(),
    monthlyRetirementNeeds = monthlyRetirementNeeds.toPlainString(),
    postRetirementIncomeTarget = postRetirementIncomeTarget.toPlainString(),
    inflationRatePercent = inflationRatePercent.toPlainString(),
    annualGoldGrowthRatePercent = annualGoldGrowthRatePercent.toPlainString(),
    targetLegacy = targetLegacy.toPlainString(),
    notes = notes,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun FinancialUpdateEntity.toDomain() = FinancialUpdate(
    id = id,
    updateDate = updateDate,
    netIncome = netIncome.toBigDecimalSafe(),
    mandatoryExpenses = mandatoryExpenses.toBigDecimalSafe(),
    lifestyleExpenses = lifestyleExpenses.toBigDecimalSafe(),
    healthExpenses = healthExpenses.toBigDecimalSafe(),
    debtPayments = debtPayments.toBigDecimalSafe(),
    remainingDebt = remainingDebt.toBigDecimalSafe(),
    notes = notes,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun FinancialUpdate.toEntity() = FinancialUpdateEntity(
    id = id,
    updateDate = updateDate,
    netIncome = netIncome.toPlainString(),
    mandatoryExpenses = mandatoryExpenses.toPlainString(),
    lifestyleExpenses = lifestyleExpenses.toPlainString(),
    healthExpenses = healthExpenses.toPlainString(),
    debtPayments = debtPayments.toPlainString(),
    remainingDebt = remainingDebt.toPlainString(),
    notes = notes,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun AssetPurchaseEntity.toDomain() = AssetPurchase(
    id = id,
    assetType = assetType.toAssetType(),
    assetName = assetName,
    quantity = quantity.toBigDecimalSafe(),
    unit = unit,
    purchasePriceRupiah = purchasePriceRupiah.toBigDecimalSafe(),
    purchaseValueRupiah = purchaseValueRupiah.toBigDecimalSafe(),
    purchaseDate = purchaseDate,
    fundingSource = fundingSource.toFundingSource(),
    notes = notes,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun AssetPurchase.toEntity() = AssetPurchaseEntity(
    id = id,
    assetType = assetType.name,
    assetName = assetName,
    quantity = quantity.toPlainString(),
    unit = unit,
    purchasePriceRupiah = purchasePriceRupiah.toPlainString(),
    purchaseValueRupiah = purchaseValueRupiah.toPlainString(),
    purchaseDate = purchaseDate,
    fundingSource = fundingSource.name,
    notes = notes,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun GoldPriceSnapshotEntity.toDomain() = GoldPriceSnapshot(
    id = id,
    pricePerGram = pricePerGram?.toBigDecimalSafe(),
    currency = currency,
    source = source,
    capturedAtIso = capturedAtIso,
    status = GoldPriceStatus.valueOf(status),
)

fun GoldPriceSnapshot.toEntity() = GoldPriceSnapshotEntity(
    id = id,
    pricePerGram = pricePerGram?.toPlainString(),
    currency = currency,
    source = source,
    capturedAtIso = capturedAtIso,
    status = status.name,
)

private fun String.toAssetType(): AssetType = runCatching { AssetType.valueOf(this) }.getOrDefault(AssetType.OTHER)
private fun String.toFundingSource(): FundingSource = runCatching { FundingSource.valueOf(this) }.getOrDefault(FundingSource.NON_SAVINGS)
private fun String.toBigDecimalSafe(): BigDecimal = toBigDecimalOrNull() ?: BigDecimal.ZERO
