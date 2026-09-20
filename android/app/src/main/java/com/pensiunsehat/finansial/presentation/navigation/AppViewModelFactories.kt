package com.pensiunsehat.finansial.presentation.navigation

import androidx.lifecycle.ViewModelProvider
import com.pensiunsehat.finansial.AppContainer
import com.pensiunsehat.finansial.presentation.assetpurchase.AssetPurchaseViewModel
import com.pensiunsehat.finansial.presentation.monthlyupdate.MonthlyUpdateViewModel
import com.pensiunsehat.finansial.presentation.profile.ProfileViewModel
import com.pensiunsehat.finansial.presentation.settings.SettingsViewModel
import com.pensiunsehat.finansial.presentation.summary.SummaryViewModel

class AppViewModelFactories(
    private val appContainer: AppContainer,
) {
    fun summary(): ViewModelProvider.Factory = SummaryViewModel.Factory(appContainer.observeFinancialSummaryUseCase)
    fun profile(): ViewModelProvider.Factory = ProfileViewModel.Factory(appContainer.financialRepository)
    fun monthlyUpdate(): ViewModelProvider.Factory = MonthlyUpdateViewModel.Factory(appContainer.financialRepository)
    fun assetPurchase(): ViewModelProvider.Factory = AssetPurchaseViewModel.Factory(appContainer.financialRepository)
    fun settings(): ViewModelProvider.Factory = SettingsViewModel.Factory(
        settingsRepository = appContainer.settingsRepository,
        financialRepository = appContainer.financialRepository,
        backgroundWorkScheduler = appContainer.backgroundWorkScheduler,
    )
}
