package com.pensiunsehat.finansial.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pensiunsehat.finansial.data.local.entity.AssetPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetPurchaseDao {
    @Query("SELECT * FROM asset_purchases ORDER BY sortDateEpochDay DESC, updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<AssetPurchaseEntity>>

    @Query("SELECT * FROM asset_purchases ORDER BY sortDateEpochDay DESC, updatedAtEpochMs DESC")
    suspend fun getAll(): List<AssetPurchaseEntity>

    @Upsert
    suspend fun upsert(entity: AssetPurchaseEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AssetPurchaseEntity>)

    @Query("DELETE FROM asset_purchases")
    suspend fun clearAll()
}
