package com.qdm.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.qdm.app.domain.model.DownloadItem
import com.qdm.app.domain.model.DownloadState

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val savePath: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val stateName: String,
    val threadCount: Int,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val referer: String?,
    val userAgent: String?,
    val customHeadersJson: String?,
    val cookies: String?,
    val username: String?,
    val password: String?,
    val speedLimitBytesPerSec: Long,
    val addedAt: Long,
    val completedAt: Long?,
    val errorMessage: String?,
    val scheduledAt: Long?,
    val isQueued: Boolean = false
) {
    fun toDomainModel(liveState: DownloadState? = null): DownloadItem {
        val resolvedState = liveState ?: stateName.toDomainState(errorMessage, scheduledAt)
        return DownloadItem(
            id = id,
            url = url,
            fileName = fileName,
            savePath = savePath,
            mimeType = mimeType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            state = resolvedState,
            threadCount = threadCount,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            referer = referer,
            userAgent = userAgent,
            customHeaders = customHeadersJson?.parseHeadersJson() ?: emptyMap(),
            cookies = cookies,
            speedLimitBytesPerSec = speedLimitBytesPerSec,
            addedAt = addedAt,
            completedAt = completedAt,
            errorMessage = errorMessage,
            scheduledAt = scheduledAt
        )
    }
}

private fun String.toDomainState(errorMessage: String?, scheduledAt: Long?): DownloadState =
    when (this) {
        "Pending" -> DownloadState.Pending
        "Connecting" -> DownloadState.Connecting
        "Paused" -> DownloadState.Paused
        "Completed" -> DownloadState.Completed
        "Cancelled" -> DownloadState.Cancelled
        "Error" -> DownloadState.Error(errorMessage ?: "Unknown error")
        "Scheduled" -> if (scheduledAt != null) DownloadState.Scheduled(scheduledAt) else DownloadState.Pending
        else -> DownloadState.Pending
    }

private fun String.parseHeadersJson(): Map<String, String> {
    return try {
        val result = mutableMapOf<String, String>()
        val trimmed = trim().removePrefix("{").removeSuffix("}")
        if (trimmed.isBlank()) return result
        trimmed.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                result[parts[0].trim().removeSurrounding("\"")] =
                    parts[1].trim().removeSurrounding("\"")
            }
        }
        result
    } catch (e: Exception) {
        emptyMap()
    }
}
