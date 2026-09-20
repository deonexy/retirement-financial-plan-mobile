package com.pensiunsehat.finansial.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pensiunsehat.finansial.data.local.dao.AssetPurchaseDao
import com.pensiunsehat.finansial.data.local.dao.FinancialUpdateDao
import com.pensiunsehat.finansial.data.local.dao.GoldPriceSnapshotDao
import com.pensiunsehat.finansial.data.local.dao.RetirementProfileDao
import com.pensiunsehat.finansial.data.local.entity.AssetPurchaseEntity
import com.pensiunsehat.finansial.data.local.entity.FinancialUpdateEntity
import com.pensiunsehat.finansial.data.local.entity.GoldPriceSnapshotEntity
import com.pensiunsehat.finansial.data.local.entity.RetirementProfileEntity

@Database(
    entities = [
        RetirementProfileEntity::class,
        FinancialUpdateEntity::class,
        AssetPurchaseEntity::class,
        GoldPriceSnapshotEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun retirementProfileDao(): RetirementProfileDao
    abstract fun financialUpdateDao(): FinancialUpdateDao
    abstract fun assetPurchaseDao(): AssetPurchaseDao
    abstract fun goldPriceSnapshotDao(): GoldPriceSnapshotDao
}
