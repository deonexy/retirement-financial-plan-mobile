package com.pensiunsehat.finansial.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pensiunsehat.finansial.R
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

        showReminderNotification()
        WorkScheduler.setMonthlyReminderEnabled(
            applicationContext,
            enabled = true,
            reminderDayOfMonth = settings.reminderDayOfMonth,
        )
        return Result.success()
    }

    private fun showReminderNotification() {
        val channelId = "monthly-reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Pengingat bulanan", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(applicationContext.getString(R.string.monthly_reminder_title))
            .setContentText(applicationContext.getString(R.string.monthly_reminder_message))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
    }
}
