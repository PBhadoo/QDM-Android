package com.qdm.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qdm.app.presentation.screens.browser.BrowserScreen
import com.qdm.app.presentation.screens.main.MainScreen
import com.qdm.app.presentation.screens.settings.SettingsScreen

@Composable
fun NavGraph(startUrl: String? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToBrowser = { url ->
                    navController.navigate(Screen.Browser.createRoute(url))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Browser.route,
            arguments = listOf(navArgument("url") {
                type = NavType.StringType
                defaultValue = "https://www.google.com"
            })
        ) { backStackEntry ->
            BrowserScreen(
                initialUrl = backStackEntry.arguments?.getString("url") ?: "https://www.google.com",
                onNavigateBack = { navController.popBackStack() },
                onDownloadRequested = { url, headers, cookies ->
                    // Navigate back to main and open add sheet pre-filled
                    navController.popBackStack()
                    // URL will be picked up by MainScreen via pendingUrl
                    com.qdm.app.MainActivity.pendingUrl = url
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
