package com.pensiunsehat.finansial.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pensiunsehat.finansial.data.local.entity.FinancialUpdateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialUpdateDao {
    @Query("SELECT * FROM financial_updates ORDER BY updateDate DESC, updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<FinancialUpdateEntity>>

    @Upsert
    suspend fun upsert(entity: FinancialUpdateEntity)
}
