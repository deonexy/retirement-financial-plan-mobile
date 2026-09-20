package com.pensiunsehat.finansial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pensiunsehat.finansial.presentation.navigation.RetirementFinanceApp
import com.pensiunsehat.finansial.presentation.theme.FinanceAppTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by appContainer.settingsRepository.settingsFlow.collectAsState(initial = appContainer.settingsRepository.defaultSettings)
            FinanceAppTheme(themePreference = settings.themePreference) {
                RetirementFinanceApp(appContainer = appContainer)
            }
        }
    }
}
