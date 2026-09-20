package com.pensiunsehat.finansial.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pensiunsehat.finansial.data.local.entity.AssetPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetPurchaseDao {
    @Query("SELECT * FROM asset_purchases ORDER BY purchaseDate DESC, updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<AssetPurchaseEntity>>

    @Upsert
    suspend fun upsert(entity: AssetPurchaseEntity)
}
