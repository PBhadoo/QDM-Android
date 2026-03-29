package com.parveenbhadoo.qdm.presentation.screens.browser

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://www.google.com",
    val title: String = "New Tab"
)
