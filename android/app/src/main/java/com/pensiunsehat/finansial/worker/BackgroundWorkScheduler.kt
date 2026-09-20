package com.pensiunsehat.finansial.worker

import android.content.Context

interface BackgroundWorkScheduler {
    fun setMonthlyReminderEnabled(enabled: Boolean, reminderDayOfMonth: Int)
    fun setGoldPriceAutoRefreshEnabled(enabled: Boolean)
}

class WorkManagerBackgroundWorkScheduler(
    private val context: Context,
) : BackgroundWorkScheduler {
    override fun setMonthlyReminderEnabled(enabled: Boolean, reminderDayOfMonth: Int) {
        WorkScheduler.setMonthlyReminderEnabled(context, enabled, reminderDayOfMonth)
    }

    override fun setGoldPriceAutoRefreshEnabled(enabled: Boolean) {
        WorkScheduler.setGoldPriceAutoRefreshEnabled(context, enabled)
    }
}
