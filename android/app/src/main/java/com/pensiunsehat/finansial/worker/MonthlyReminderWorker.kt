package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MonthlyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val reminderDayOfMonth = inputData.getInt(WorkScheduler.REMINDER_DAY_OF_MONTH, 1)
        WorkScheduler.setMonthlyReminderEnabled(applicationContext, enabled = true, reminderDayOfMonth = reminderDayOfMonth)
        return Result.success()
    }
}
