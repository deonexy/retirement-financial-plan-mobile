package com.pensiunsehat.finansial.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {
    val defaultSettings = AppSettings()

    val settingsFlow: Flow<AppSettings> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings(
            themePreference = prefs[THEME]?.let(ThemePreference::valueOf) ?: ThemePreference.SYSTEM,
            monthlyReminderEnabled = prefs[MONTHLY_REMINDER_ENABLED] ?: false,
            reminderDayOfMonth = prefs[REMINDER_DAY_OF_MONTH] ?: 1,
            goldPriceAutoRefresh = prefs[GOLD_PRICE_AUTO_REFRESH] ?: false,
        )
    }

    suspend fun setThemePreference(value: ThemePreference) {
        context.appSettingsDataStore.edit { it[THEME] = value.name }
    }

    suspend fun setMonthlyReminderEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { it[MONTHLY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderDayOfMonth(day: Int) {
        context.appSettingsDataStore.edit { it[REMINDER_DAY_OF_MONTH] = day.coerceIn(1, 28) }
    }

    suspend fun setGoldPriceAutoRefresh(enabled: Boolean) {
        context.appSettingsDataStore.edit { it[GOLD_PRICE_AUTO_REFRESH] = enabled }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val MONTHLY_REMINDER_ENABLED = booleanPreferencesKey("monthly_reminder_enabled")
        val REMINDER_DAY_OF_MONTH = intPreferencesKey("reminder_day_of_month")
        val GOLD_PRICE_AUTO_REFRESH = booleanPreferencesKey("gold_price_auto_refresh")
    }
}
