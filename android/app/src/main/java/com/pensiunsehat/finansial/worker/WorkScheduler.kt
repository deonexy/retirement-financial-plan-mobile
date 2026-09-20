package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pensiunsehat.finansial.domain.calculator.MonthlyReminderScheduleCalculator
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
    internal const val REMINDER_DAY_OF_MONTH = "reminder_day_of_month"
    private const val MONTHLY_REMINDER_WORK = "monthly-reminder"
    private const val GOLD_PRICE_REFRESH_WORK = "gold-price-refresh"

    fun setMonthlyReminderEnabled(context: Context, enabled: Boolean, reminderDayOfMonth: Int) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(MONTHLY_REMINDER_WORK)
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
            MONTHLY_REMINDER_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun setGoldPriceAutoRefreshEnabled(context: Context, enabled: Boolean) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(GOLD_PRICE_REFRESH_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<GoldPriceRefreshWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            GOLD_PRICE_REFRESH_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

}