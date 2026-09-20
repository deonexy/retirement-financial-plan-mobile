package com.pensiunsehat.finansial.data.repository

import com.pensiunsehat.finansial.data.local.dao.AssetPurchaseDao
import com.pensiunsehat.finansial.data.local.dao.FinancialUpdateDao
import com.pensiunsehat.finansial.data.local.dao.GoldPriceSnapshotDao
import com.pensiunsehat.finansial.data.local.dao.RetirementProfileDao
import com.pensiunsehat.finansial.domain.calculator.RetirementCalculator
import com.pensiunsehat.finansial.domain.model.AssetPurchase
import com.pensiunsehat.finansial.domain.model.FinancialSummary
import com.pensiunsehat.finansial.domain.model.FinancialUpdate
import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomLocalFinancialRepository(
    private val retirementProfileDao: RetirementProfileDao,
    private val financialUpdateDao: FinancialUpdateDao,
    private val assetPurchaseDao: AssetPurchaseDao,
    private val goldPriceSnapshotDao: GoldPriceSnapshotDao,
) : LocalFinancialRepository {
    override fun observeProfile(): Flow<RetirementProfile?> = retirementProfileDao.observeLatest().map { it?.toDomain() }

    override fun observeFinancialUpdates(): Flow<List<FinancialUpdate>> = financialUpdateDao.observeAll().map { items ->
        items.map { it.toDomain() }
    }

    override fun observeAssetPurchases(): Flow<List<AssetPurchase>> = assetPurchaseDao.observeAll().map { items ->
        items.map { it.toDomain() }
    }

    override fun observeGoldPriceSnapshot(): Flow<GoldPriceSnapshot?> = goldPriceSnapshotDao.observeLatest().map { it?.toDomain() }

    override fun observeFinancialSummary(): Flow<FinancialSummary> = combine(
        observeProfile(),
        observeFinancialUpdates(),
        observeAssetPurchases(),
        observeGoldPriceSnapshot(),
    ) { profile, updates, purchases, goldSnapshot ->
        RetirementCalculator.calculateSummary(
            profile = profile ?: RetirementProfile(),
            updates = updates,
            purchases = purchases,
            goldPriceSnapshot = goldSnapshot,
        )
    }

    override suspend fun saveProfile(profile: RetirementProfile) {
        retirementProfileDao.upsert(profile.toEntity())
    }

    override suspend fun saveFinancialUpdate(update: FinancialUpdate) {
        financialUpdateDao.upsert(update.toEntity())
    }

    override suspend fun saveAssetPurchase(purchase: AssetPurchase) {
        assetPurchaseDao.upsert(purchase.toEntity())
    }

    override suspend fun saveGoldPriceSnapshot(snapshot: GoldPriceSnapshot) {
        goldPriceSnapshotDao.upsert(snapshot.toEntity())
    }
}
