package com.pensiunsehat.finansial.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pensiunsehat.finansial.data.local.entity.GoldPriceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldPriceSnapshotDao {
    @Query("SELECT * FROM gold_price_snapshots ORDER BY capturedAtEpochDay DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<GoldPriceSnapshotEntity?>

    @Upsert
    suspend fun upsert(entity: GoldPriceSnapshotEntity)
}
