package com.qdm.app.domain.engine

import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import kotlin.coroutines.coroutineContext

class ChunkDownloader(
    private val client: OkHttpClient,
    private val url: String,
    private val startByte: Long,
    private val endByte: Long,
    private val fileDescriptor: FileDescriptor,
    private val extraHeaders: Map<String, String> = emptyMap(),
    private val progressCallback: suspend (bytesRead: Long) -> Unit
) {
    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_INTERVAL_BYTES = 512 * 1024L
    }

    suspend fun download(): Result<Unit> = runCatching {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Range", "bytes=$startByte-$endByte")
            .header("Accept-Encoding", "identity") // Prevent gzip from breaking byte accounting
        extraHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}: ${response.message}")
        }

        val body = response.body ?: throw IllegalStateException("Empty response body")
        val channel: FileChannel = FileOutputStream(fileDescriptor).channel
        channel.position(startByte)

        val buffer = ByteArray(BUFFER_SIZE)
        var accumulatedBytes = 0L

        body.byteStream().use { stream ->
            while (coroutineContext.isActive) {
                val read = stream.read(buffer)
                if (read == -1) break
                channel.write(java.nio.ByteBuffer.wrap(buffer, 0, read))
                accumulatedBytes += read
                if (accumulatedBytes >= PROGRESS_INTERVAL_BYTES) {
                    progressCallback(accumulatedBytes)
                    accumulatedBytes = 0L
                }
            }
            if (accumulatedBytes > 0) progressCallback(accumulatedBytes)
        }
    }
}
