package com.pensiunsehat.finansial.worker

import android.content.Context

interface BackgroundWorkScheduler {
    fun setMonthlyReminderEnabled(enabled: Boolean, reminderDayOfMonth: Int)
    fun setGoldPriceRefreshEnabled(enabled: Boolean)
}

class WorkManagerBackgroundWorkScheduler(
    private val context: Context,
) : BackgroundWorkScheduler {
    override fun setMonthlyReminderEnabled(enabled: Boolean, reminderDayOfMonth: Int) {
        WorkScheduler.setMonthlyReminderEnabled(context, enabled, reminderDayOfMonth)
    }

    override fun setGoldPriceRefreshEnabled(enabled: Boolean) {
        WorkScheduler.setGoldPriceRefreshEnabled(context, enabled)
    }
}
