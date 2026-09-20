package com.pensiunsehat.finansial.worker

import android.content.Context

interface BackgroundWorkScheduler {
    fun setMonthlyReminderEnabled(enabled: Boolean)
    fun setGoldPriceRefreshEnabled(enabled: Boolean)
}

class WorkManagerBackgroundWorkScheduler(
    private val context: Context,
) : BackgroundWorkScheduler {
    override fun setMonthlyReminderEnabled(enabled: Boolean) {
        WorkScheduler.setMonthlyReminderEnabled(context, enabled)
    }

    override fun setGoldPriceRefreshEnabled(enabled: Boolean) {
        WorkScheduler.setGoldPriceRefreshEnabled(context, enabled)
    }
}
