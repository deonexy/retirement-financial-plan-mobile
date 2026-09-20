package com.pensiunsehat.finansial.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pensiunsehat.finansial.AppContainer
import com.pensiunsehat.finansial.presentation.assetpurchase.AssetPurchaseScreen
import com.pensiunsehat.finansial.presentation.assetpurchase.AssetPurchaseViewModel
import com.pensiunsehat.finansial.presentation.monthlyupdate.MonthlyUpdateScreen
import com.pensiunsehat.finansial.presentation.monthlyupdate.MonthlyUpdateViewModel
import com.pensiunsehat.finansial.presentation.profile.ProfileScreen
import com.pensiunsehat.finansial.presentation.profile.ProfileViewModel
import com.pensiunsehat.finansial.presentation.settings.SettingsScreen
import com.pensiunsehat.finansial.presentation.settings.SettingsViewModel
import com.pensiunsehat.finansial.presentation.summary.SummaryScreen
import com.pensiunsehat.finansial.presentation.summary.SummaryViewModel

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    SUMMARY("summary", "Summary", Icons.Filled.Home),
    PROFILE("profile", "Profile", Icons.Filled.Person),
    MONTHLY_UPDATE("monthly-update", "Update", Icons.Filled.DateRange),
    ASSET_PURCHASE("asset-purchase", "Aset", Icons.Filled.AccountBalanceWallet),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun RetirementFinanceApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val factories = AppViewModelFactories(appContainer)
    val backStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = backStackEntry?.destination
    val destinations = AppDestination.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.SUMMARY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.SUMMARY.route) {
                val viewModel: SummaryViewModel = viewModel(
                    factory = factories.summary(),
                )
                SummaryScreen(viewModel)
            }
            composable(AppDestination.PROFILE.route) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = factories.profile(),
                )
                ProfileScreen(viewModel)
            }
            composable(AppDestination.MONTHLY_UPDATE.route) {
                val viewModel: MonthlyUpdateViewModel = viewModel(
                    factory = factories.monthlyUpdate(),
                )
                MonthlyUpdateScreen(viewModel)
            }
            composable(AppDestination.ASSET_PURCHASE.route) {
                val viewModel: AssetPurchaseViewModel = viewModel(
                    factory = factories.assetPurchase(),
                )
                AssetPurchaseScreen(viewModel)
            }
            composable(AppDestination.SETTINGS.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = factories.settings(),
                )
                SettingsScreen(viewModel)
            }
        }
    }
}
