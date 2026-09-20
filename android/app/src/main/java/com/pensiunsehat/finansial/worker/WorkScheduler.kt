package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun setMonthlyReminderEnabled(context: Context, enabled: Boolean) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork("monthly-reminder")
            return
        }

        val request = PeriodicWorkRequestBuilder<MonthlyReminderWorker>(30, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "monthly-reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun setGoldPriceRefreshEnabled(context: Context, enabled: Boolean) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork("gold-price-refresh")
            return
        }

        val request = PeriodicWorkRequestBuilder<GoldPriceRefreshWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "gold-price-refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
