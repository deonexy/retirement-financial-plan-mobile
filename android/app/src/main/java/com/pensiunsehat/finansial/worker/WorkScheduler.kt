package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pensiunsehat.finansial.domain.calculator.MonthlyReminderScheduleCalculator
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
    internal const val REMINDER_DAY_OF_MONTH = "reminder_day_of_month"

    fun setMonthlyReminderEnabled(context: Context, enabled: Boolean, reminderDayOfMonth: Int) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork("monthly-reminder")
            return
        }

        val day = reminderDayOfMonth.coerceIn(1, 28)
        val request = OneTimeWorkRequestBuilder<MonthlyReminderWorker>()
            .setInputData(
                Data.Builder()
                    .putInt(REMINDER_DAY_OF_MONTH, day)
                    .build(),
            )
            .setInitialDelay(MonthlyReminderScheduleCalculator.delayMillis(ZonedDateTime.now(), day), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "monthly-reminder",
            ExistingWorkPolicy.REPLACE,
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
