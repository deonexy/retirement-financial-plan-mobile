package com.pensiunsehat.finansial

import android.content.Context
import androidx.room.Room
import com.pensiunsehat.finansial.data.local.AppDatabase
import com.pensiunsehat.finansial.data.repository.LocalFinancialRepository
import com.pensiunsehat.finansial.data.repository.RoomLocalFinancialRepository
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import com.pensiunsehat.finansial.domain.usecase.ObserveFinancialSummaryUseCase

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "retirement_finance.db",
    ).build()

    val financialRepository: LocalFinancialRepository = RoomLocalFinancialRepository(
        retirementProfileDao = database.retirementProfileDao(),
        financialUpdateDao = database.financialUpdateDao(),
        assetPurchaseDao = database.assetPurchaseDao(),
        goldPriceSnapshotDao = database.goldPriceSnapshotDao(),
    )
    val settingsRepository = SettingsRepository(context)
    val observeFinancialSummaryUseCase = ObserveFinancialSummaryUseCase(financialRepository)
}
