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
import java.time.Duration
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
            .setInitialDelay(calculateNextMonthlyDelay(day), TimeUnit.MILLISECONDS)
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

    private fun calculateNextMonthlyDelay(reminderDayOfMonth: Int): Long {
        val now = ZonedDateTime.now()
        val todayTarget = now.withDayOfMonth(reminderDayOfMonth.coerceAtMost(now.toLocalDate().lengthOfMonth()))
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val nextRun = if (todayTarget.isAfter(now)) {
            todayTarget
        } else {
            val nextMonth = now.plusMonths(1)
            nextMonth.withDayOfMonth(reminderDayOfMonth.coerceAtMost(nextMonth.toLocalDate().lengthOfMonth()))
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        }
        return Duration.between(now, nextRun).toMillis().coerceAtLeast(0)
    }
}
