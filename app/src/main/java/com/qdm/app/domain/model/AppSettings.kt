package com.qdm.app.domain.model

data class AppSettings(
    val defaultSavePath: String = "",
    val defaultThreadCount: Int = 4,
    val maxConcurrentDownloads: Int = 3,
    val globalSpeedLimitBps: Long = 0L,
    val wifiOnlyMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dynamicTheme: Boolean = true,
    val darkMode: Boolean? = null,
    val defaultUserAgent: String = DEFAULT_USER_AGENT,
    val appLanguage: String = "system"
) {
    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
    }
}
