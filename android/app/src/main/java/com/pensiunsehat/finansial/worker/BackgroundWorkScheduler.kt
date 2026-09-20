package com.pensiunsehat.finansial.worker

import android.content.Context

interface BackgroundWorkScheduler {
    fun setMonthlyReminderEnabled(enabled: Boolean, reminderDayOfMonth: Int)
}

class WorkManagerBackgroundWorkScheduler(
    private val context: Context,
) : BackgroundWorkScheduler {
    override fun setMonthlyReminderEnabled(enabled: Boolean, reminderDayOfMonth: Int) {
        WorkScheduler.setMonthlyReminderEnabled(context, enabled, reminderDayOfMonth)
    }
}
