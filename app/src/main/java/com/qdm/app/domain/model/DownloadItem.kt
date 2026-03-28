package com.parveenbhadoo.qdm.domain.model

data class DownloadItem(
    val id: String,
    val url: String,
    val fileName: String,
    val savePath: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val state: DownloadState,
    val threadCount: Int,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val referer: String?,
    val userAgent: String?,
    val customHeaders: Map<String, String>,
    val cookies: String?,
    val username: String?,
    val password: String?,
    val speedLimitBytesPerSec: Long,
    val addedAt: Long,
    val completedAt: Long?,
    val errorMessage: String?,
    val scheduledAt: Long?,
    val supportsRanges: Boolean = false
) {
    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

    val notificationId: Int
        get() = id.hashCode()
}
