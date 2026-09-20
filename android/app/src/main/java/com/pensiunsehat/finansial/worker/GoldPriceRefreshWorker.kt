package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pensiunsehat.finansial.data.local.AppDatabase
import com.pensiunsehat.finansial.data.local.entity.GoldPriceSnapshotEntity
import com.pensiunsehat.finansial.data.remote.DefaultGoldPriceApi
import com.pensiunsehat.finansial.data.remote.GoldPriceApi
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import com.pensiunsehat.finansial.domain.model.GoldPriceStatus
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first

class GoldPriceRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).settingsFlow.first()
        if (!settings.goldPriceAutoRefresh) return Result.success()

        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "retirement_finance.db",
        ).build()
        return try {
            val latest = database.goldPriceSnapshotDao().getLatest()
            val payload = runCatching { apiFactory().fetchLatestGoldPrice() }.getOrNull()
            val snapshot = if (payload != null) {
                GoldPriceSnapshotEntity(
                    pricePerGram = payload.pricePerGram,
                    currency = payload.currency,
                    source = payload.source,
                    capturedAtIso = payload.capturedAtIso,
                    capturedAtEpochDay = payload.capturedAtIso.toEpochDaySafe(),
                    status = GoldPriceStatus.LIVE.name,
                )
            } else if (latest?.pricePerGram != null) {
                GoldPriceSnapshotEntity(
                    pricePerGram = latest.pricePerGram,
                    currency = latest.currency,
                    source = latest.source,
                    capturedAtIso = latest.capturedAtIso ?: LocalDate.now(ZoneOffset.UTC).toString(),
                    capturedAtEpochDay = latest.capturedAtEpochDay ?: LocalDate.now(ZoneOffset.UTC).toEpochDay(),
                    status = GoldPriceStatus.LAST_KNOWN.name,
                )
            } else {
                GoldPriceSnapshotEntity(
                    pricePerGram = null,
                    currency = "IDR",
                    source = "unavailable",
                    capturedAtIso = LocalDate.now(ZoneOffset.UTC).toString(),
                    capturedAtEpochDay = LocalDate.now(ZoneOffset.UTC).toEpochDay(),
                    status = GoldPriceStatus.UNAVAILABLE.name,
                )
            }
            database.goldPriceSnapshotDao().upsert(snapshot)
            Result.success()
        } finally {
            database.close()
        }
    }

    private fun String?.toEpochDaySafe(): Long? = this?.runCatching { LocalDate.parse(this).toEpochDay() }?.getOrNull()

    internal companion object {
        @Volatile
        var apiFactory: () -> GoldPriceApi = { DefaultGoldPriceApi() }
    }
}
