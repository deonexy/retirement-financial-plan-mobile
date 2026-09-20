package com.pensiunsehat.finansial.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gold_price_snapshots")
data class GoldPriceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pricePerGram: String?,
    val currency: String,
    val source: String,
    val capturedAtIso: String?,
    val capturedAtEpochDay: Long? = null,
    val status: String,
)
