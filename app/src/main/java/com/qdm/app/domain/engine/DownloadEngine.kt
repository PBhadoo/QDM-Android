package com.qdm.app.domain.engine

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.qdm.app.data.repository.DownloadRepository
import com.qdm.app.domain.model.DownloadItem
import com.qdm.app.domain.model.DownloadState
import com.qdm.app.utils.FileUtils
import com.qdm.app.utils.QdmLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val repository: DownloadRepository
) {
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val progressMutex = Mutex()

    private val _stateMap = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val stateFlow: StateFlow<Map<String, DownloadState>> = _stateMap.asStateFlow()

    fun hasActiveDownloads(): Boolean = activeJobs.isNotEmpty()

    fun startDownload(downloadId: String, item: DownloadItem, scope: CoroutineScope) {
        if (activeJobs.containsKey(downloadId)) return
        QdmLog.i("DownloadEngine", "startDownload id=$downloadId url=${item.url} threads=${item.threadCount}")

        val job = scope.launch(Dispatchers.IO) {
            try {
                updateState(downloadId, DownloadState.Connecting)
                repository.updateState(downloadId, DownloadState.Connecting)

                // Resolve file output
                val fileUri = resolveOutputUri(item) ?: run {
                    QdmLog.e("DownloadEngine", "resolveOutputUri returned null for savePath='${item.savePath}'")
                    fail(downloadId, "Cannot create output file")
                    return@launch
                }
                QdmLog.d("DownloadEngine", "Output URI resolved: $fileUri")

                val pfd = FileUtils.openFileDescriptorForWrite(context, fileUri) ?: run {
                    QdmLog.e("DownloadEngine", "openFileDescriptorForWrite returned null for $fileUri")
                    fail(downloadId, "Cannot open file for writing")
                    return@launch
                }

                // pfd.use ensures ParcelFileDescriptor stays alive (and open) for the entire
                // download, preventing the GC from closing the underlying fd prematurely.
                pfd.use { descriptor ->
                val speedCalc = SpeedCalculator()
                var downloadedBytes = item.downloadedBytes
                val totalBytes = item.totalBytes

                updateState(downloadId, DownloadState.Downloading(
                    progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f,
                    speedBps = 0L, etaSeconds = 0L,
                    downloadedBytes = downloadedBytes, totalBytes = totalBytes
                ))

                val supportsRanges = totalBytes > 0 && item.threadCount > 1
                val threadCount = if (supportsRanges) item.threadCount else 1

                supervisorScope {
                    val chunks = if (supportsRanges && totalBytes > 0) {
                        splitIntoChunks(downloadedBytes, totalBytes, threadCount)
                    } else {
                        listOf(Pair(downloadedBytes, -1L))
                    }

                    val chunkJobs = chunks.map { (start, end) ->
                        async {
                            ChunkDownloader(
                                client = client,
                                url = item.url,
                                startByte = start,
                                endByte = end,
                                fileDescriptor = descriptor.fileDescriptor,
                                extraHeaders = buildHeaders(item),
                                progressCallback = { bytes ->
                                    if (!isActive) return@ChunkDownloader
                                    progressMutex.withLock {
                                        downloadedBytes += bytes
                                        speedCalc.record(bytes)
                                        val speed = speedCalc.speedBps()
                                        val eta = speedCalc.etaSeconds(
                                            (totalBytes - downloadedBytes).coerceAtLeast(0L)
                                        )
                                        val progress = if (totalBytes > 0)
                                            downloadedBytes.toFloat() / totalBytes else 0f
                                        updateState(downloadId, DownloadState.Downloading(
                                            progress, speed, eta, downloadedBytes, totalBytes
                                        ))
                                    }
                                    repository.updateProgress(downloadId, downloadedBytes, speedCalc.speedBps(), speedCalc.etaSeconds((totalBytes - downloadedBytes).coerceAtLeast(0L)))
                                }
                            ).download()
                        }
                    }

                    val results = chunkJobs.awaitAll()
                    val failed = results.firstOrNull { it.isFailure }
                    if (failed != null) {
                        throw failed.exceptionOrNull() ?: Exception("Chunk failed")
                    }
                }
                } // end pfd.use

                if (isActive) {
                    FileUtils.markFileDownloadComplete(context, fileUri)
                    repository.markCompleted(downloadId)
                    updateState(downloadId, DownloadState.Completed)
                    QdmLog.i("DownloadEngine", "Completed id=$downloadId")
                }
            } catch (e: Exception) {
                if (activeJobs.containsKey(downloadId)) {
                    QdmLog.e("DownloadEngine", "Failed id=$downloadId: ${e.message}", e)
                    fail(downloadId, e.message ?: "Download failed")
                }
            } finally {
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: String) {
        activeJobs.remove(downloadId)?.cancel()
        updateState(downloadId, DownloadState.Paused)
    }

    fun cancelDownload(downloadId: String) {
        activeJobs.remove(downloadId)?.cancel()
        updateState(downloadId, DownloadState.Cancelled)
    }

    private fun fail(downloadId: String, message: String) {
        updateState(downloadId, DownloadState.Error(message))
    }

    private fun updateState(id: String, state: DownloadState) {
        _stateMap.update { current -> current + (id to state) }
    }

    private fun resolveOutputUri(item: DownloadItem): Uri? {
        val savePath = item.savePath
        if (savePath.isBlank()) {
            QdmLog.d("DownloadEngine", "No savePath — using default Downloads/QDM/${FileUtils.categoryFolder(item.mimeType)}/")
            return FileUtils.createFileForDownload(context, item.fileName, item.mimeType)
        }

        val uri = Uri.parse(savePath)
        return if (savePath.startsWith("content://") && !DocumentsContract.isTreeUri(uri)) {
            // Already a specific document URI (e.g. previously saved MediaStore entry)
            QdmLog.d("DownloadEngine", "Using existing document URI: $uri")
            uri
        } else {
            // SAF tree URI — must create a child document inside it
            QdmLog.d("DownloadEngine", "SAF tree URI — creating child document in $uri")
            FileUtils.createDocumentFile(context, uri, item.fileName, item.mimeType)
                ?: FileUtils.createFileForDownload(context, item.fileName, item.mimeType)
        }
    }

    private fun buildHeaders(item: DownloadItem): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        item.referer?.let { headers["Referer"] = it }
        item.userAgent?.let { headers["User-Agent"] = it }
        item.cookies?.let { headers["Cookie"] = it }
        item.customHeaders.forEach { (k, v) -> headers[k] = v }
        if (item.username != null && item.password != null) {
            val creds = android.util.Base64.encodeToString(
                "${item.username}:${item.password}".toByteArray(), android.util.Base64.NO_WRAP
            )
            headers["Authorization"] = "Basic $creds"
        }
        return headers
    }

    private fun splitIntoChunks(
        startByte: Long,
        totalBytes: Long,
        threadCount: Int
    ): List<Pair<Long, Long>> {
        val remaining = totalBytes - startByte
        val chunkSize = remaining / threadCount
        return (0 until threadCount).map { i ->
            val start = startByte + i * chunkSize
            val end = if (i == threadCount - 1) totalBytes - 1 else start + chunkSize - 1
            Pair(start, end)
        }
    }
}
