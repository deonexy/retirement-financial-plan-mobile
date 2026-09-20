package com.pensiunsehat.finansial.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset_purchases")
data class AssetPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetType: String,
    val assetName: String,
    val quantity: String,
    val unit: String,
    val purchasePriceRupiah: String,
    val purchaseValueRupiah: String,
    val purchaseDate: String,
    val sortDateEpochDay: Long = 0,
    val fundingSource: String,
    val notes: String?,
    val updatedAtEpochMs: Long,
)
