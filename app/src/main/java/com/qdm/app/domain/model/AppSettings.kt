package com.parveenbhadoo.qdm.domain.model

data class AppSettings(
    val defaultSavePath: String = "",
    val folderSetupDone: Boolean = false,
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
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        const val DEFAULT_USER_AGENT = MOBILE_USER_AGENT
    }
}
