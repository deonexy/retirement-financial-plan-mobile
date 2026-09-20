package com.pensiunsehat.finansial.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pensiunsehat.finansial.data.local.entity.RetirementProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RetirementProfileDao {
    @Query("SELECT * FROM retirement_profiles WHERE id = 1 LIMIT 1")
    fun observeLatest(): Flow<RetirementProfileEntity?>

    @Upsert
    suspend fun upsert(entity: RetirementProfileEntity)
}
