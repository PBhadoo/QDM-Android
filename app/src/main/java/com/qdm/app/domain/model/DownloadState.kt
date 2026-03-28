package com.parveenbhadoo.qdm.domain.model

sealed class DownloadState {
    object Pending : DownloadState()
    object Connecting : DownloadState()
    data class Downloading(
        val progress: Float,
        val speedBps: Long,
        val etaSeconds: Long,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    object Paused : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String, val isRetryable: Boolean = true) : DownloadState()
    object Cancelled : DownloadState()
    data class Scheduled(val scheduledAt: Long) : DownloadState()
}
