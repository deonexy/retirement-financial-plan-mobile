package com.pensiunsehat.finansial.data.repository

import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.FinancialSummary
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import kotlinx.coroutines.flow.Flow

interface LocalFinancialRepository {
    fun observeProfile(): Flow<RetirementProfile?>
    fun observeFinancialUpdates(): Flow<List<FinancialUpdate>>
    fun observeAssetPurchases(): Flow<List<AssetPurchase>>
    fun observeGoldPriceSnapshot(): Flow<GoldPriceSnapshot?>
    fun observeFinancialSummary(): Flow<FinancialSummary>

    suspend fun saveProfile(profile: RetirementProfile)
    suspend fun saveFinancialUpdate(update: FinancialUpdate)
    suspend fun saveAssetPurchase(purchase: AssetPurchase)
    suspend fun saveGoldPriceSnapshot(snapshot: GoldPriceSnapshot)
}
