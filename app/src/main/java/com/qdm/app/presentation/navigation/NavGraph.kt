package com.parveenbhadoo.qdm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.parveenbhadoo.qdm.presentation.screens.browser.BrowserScreen
import com.parveenbhadoo.qdm.presentation.screens.browser.BrowserViewModel
import com.parveenbhadoo.qdm.presentation.screens.main.MainScreen
import com.parveenbhadoo.qdm.presentation.screens.settings.SettingsScreen

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
            val viewModel: BrowserViewModel = hiltViewModel()
            val navArgUrl = backStackEntry.arguments?.getString("url") ?: "https://www.google.com"
            val effectiveUrl = if (navArgUrl != "https://www.google.com") navArgUrl else viewModel.startUrl

            BrowserScreen(
                initialUrl = effectiveUrl,
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
