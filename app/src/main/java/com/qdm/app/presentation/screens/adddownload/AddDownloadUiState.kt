package com.parveenbhadoo.qdm.presentation.screens.adddownload

data class AddDownloadUiState(
    val url: String = "",
    val referer: String = "",
    val fileName: String = "",
    val savePath: String = "",
    val totalBytes: Long = -1L,
    val mimeType: String = "",
    val threadCount: Int = 4,
    val userAgent: String = "",
    val speedLimitBps: Long = 0L,
    val username: String = "",
    val password: String = "",
    val scheduledAt: Long? = null,
    val showAdvanced: Boolean = false,
    val isLoading: Boolean = false,
    val isFetched: Boolean = false,
    val error: String? = null,
    val cookies: String = "",
    val supportsRanges: Boolean = false
)
