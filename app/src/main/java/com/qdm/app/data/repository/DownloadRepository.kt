package com.qdm.app.data.repository

import com.qdm.app.data.local.dao.DownloadDao
import com.qdm.app.data.local.entity.DownloadEntity
import com.qdm.app.domain.model.AddDownloadRequest
import com.qdm.app.domain.model.DownloadItem
import com.qdm.app.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val dao: DownloadDao
) {
    fun getAllDownloads(): Flow<List<DownloadItem>> =
        dao.getAllDownloads().map { list -> list.map { it.toDomainModel() } }

    suspend fun getDownloadById(id: String): DownloadItem? =
        dao.getDownloadById(id)?.toDomainModel()

    suspend fun addDownload(request: AddDownloadRequest): String {
        val id = UUID.randomUUID().toString()
        dao.insertDownload(
            DownloadEntity(
                id = id,
                url = request.url,
                fileName = request.fileName,
                savePath = request.savePath,
                mimeType = request.mimeType,
                totalBytes = request.totalBytes,
                downloadedBytes = 0L,
                stateName = if (request.scheduledAt != null) "Scheduled" else "Pending",
                threadCount = request.threadCount,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                referer = request.referer,
                userAgent = request.userAgent,
                customHeadersJson = request.customHeaders.toJsonString(),
                cookies = request.cookies,
                username = request.username,
                password = request.password,
                speedLimitBytesPerSec = request.speedLimitBps,
                addedAt = System.currentTimeMillis(),
                completedAt = null,
                errorMessage = null,
                scheduledAt = request.scheduledAt
            )
        )
        return id
    }

    suspend fun updateState(id: String, state: DownloadState) {
        val stateName = state::class.simpleName ?: "Pending"
        val errorMessage = (state as? DownloadState.Error)?.message
        dao.updateState(id, stateName, errorMessage)
    }

    suspend fun updateProgress(id: String, downloadedBytes: Long, speed: Long, eta: Long) =
        dao.updateProgress(id, downloadedBytes, speed, eta)

    suspend fun markCompleted(id: String) =
        dao.markCompleted(id, System.currentTimeMillis())

    suspend fun deleteDownload(id: String) = dao.deleteDownload(id)

    suspend fun getActiveDownloadCount(): Int = dao.getActiveDownloadCount()

    suspend fun getPendingDownloads(): List<DownloadItem> =
        dao.getPendingDownloads().map { it.toDomainModel() }
}

private fun Map<String, String>.toJsonString(): String? {
    if (isEmpty()) return null
    return "{${entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }}}"
}
