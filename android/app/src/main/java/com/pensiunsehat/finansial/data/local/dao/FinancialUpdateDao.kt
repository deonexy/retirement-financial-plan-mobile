package com.pensiunsehat.finansial.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pensiunsehat.finansial.data.local.entity.FinancialUpdateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialUpdateDao {
    @Query("SELECT * FROM financial_updates ORDER BY sortDateEpochDay DESC, updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<FinancialUpdateEntity>>

    @Query("SELECT * FROM financial_updates ORDER BY sortDateEpochDay DESC, updatedAtEpochMs DESC")
    suspend fun getAll(): List<FinancialUpdateEntity>

    @Upsert
    suspend fun upsert(entity: FinancialUpdateEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FinancialUpdateEntity>)

    @Query("DELETE FROM financial_updates")
    suspend fun clearAll()
}
