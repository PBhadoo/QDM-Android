package com.parveenbhadoo.qdm.domain.model

data class AddDownloadRequest(
    val url: String,
    val fileName: String,
    val savePath: String,
    val mimeType: String = "",
    val totalBytes: Long = -1L,
    val referer: String? = null,
    val userAgent: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
    val cookies: String? = null,
    val threadCount: Int = 4,
    val speedLimitBps: Long = 0L,
    val username: String? = null,
    val password: String? = null,
    val scheduledAt: Long? = null,
    val supportsRanges: Boolean = false
)
