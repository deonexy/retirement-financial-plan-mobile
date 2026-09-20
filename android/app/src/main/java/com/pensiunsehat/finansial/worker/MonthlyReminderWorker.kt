package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class MonthlyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).settingsFlow.first()
        if (!settings.monthlyReminderEnabled) {
            WorkScheduler.setMonthlyReminderEnabled(applicationContext, enabled = false, reminderDayOfMonth = settings.reminderDayOfMonth)
            return Result.success()
        }

        WorkScheduler.setMonthlyReminderEnabled(
            applicationContext,
            enabled = true,
            reminderDayOfMonth = settings.reminderDayOfMonth,
        )
        return Result.success()
    }
}
