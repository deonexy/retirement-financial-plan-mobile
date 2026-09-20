package com.pensiunsehat.finansial.data.repository

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val monthlyReminderEnabled: Boolean = false,
    val reminderDayOfMonth: Int = 1,
    val goldPriceAutoRefresh: Boolean = false,
)
