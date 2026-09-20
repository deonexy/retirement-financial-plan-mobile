package com.pensiunsehat.finansial.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

        val notificationPosted = showReminderNotification()
        if (!notificationPosted) {
            WorkScheduler.setMonthlyReminderEnabled(
                applicationContext,
                enabled = true,
                reminderDayOfMonth = settings.reminderDayOfMonth,
            )
            return Result.success()
        }

        WorkScheduler.setMonthlyReminderEnabled(
            applicationContext,
            enabled = true,
            reminderDayOfMonth = settings.reminderDayOfMonth,
        )
        return Result.success()
    }

    private fun showReminderNotification(): Boolean {
        val channelId = "monthly-reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Pengingat bulanan", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(applicationContext.getString(R.string.monthly_reminder_title))
            .setContentText(applicationContext.getString(R.string.monthly_reminder_message))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        return true
    }
}
