package com.pensiunsehat.finansial.domain.model

import java.math.BigDecimal

data class AssetPurchase(
    val id: Long = 0,
    val assetType: AssetType,
    val assetName: String,
    val quantity: BigDecimal,
    val unit: String,
    val purchasePriceRupiah: BigDecimal,
    val purchaseValueRupiah: BigDecimal,
    val purchaseDate: String,
    val fundingSource: FundingSource,
    val notes: String? = null,
    val updatedAtEpochMs: Long = 0,
)
