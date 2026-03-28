package com.qdm.app.presentation.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Browser : Screen("browser?url={url}") {
        fun createRoute(url: String = "https://www.google.com") = "browser?url=$url"
    }
    object Settings : Screen("settings")
}
