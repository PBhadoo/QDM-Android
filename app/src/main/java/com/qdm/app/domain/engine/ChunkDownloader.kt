package com.parveenbhadoo.qdm.domain.engine

import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileDescriptor
import java.io.FileOutputStream
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
            .header("Accept-Encoding", "identity") // Prevent gzip from breaking byte accounting
        extraHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }
        // Only add Range header when actually requesting a sub-range.
        // startByte=0 + endByte=-1 means "full file" — sending Range: bytes=0--1 causes HTTP 416.
        if (startByte > 0 || endByte >= 0) {
            val end = if (endByte >= 0) "$endByte" else ""
            requestBuilder.header("Range", "bytes=$startByte-$end")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}: ${resp.message}")
            }

            // If we sent a Range header but got 200 (not 206), the server ignored the range.
            // Treat this as a full-file response starting at 0 to avoid writing corrupt data.
            val actualStartByte = if (startByte > 0 && resp.code == 200) 0L else startByte

            val body = resp.body ?: throw IllegalStateException("Empty response body")
            val buffer = ByteArray(BUFFER_SIZE)
            var accumulatedBytes = 0L

            FileOutputStream(fileDescriptor).channel.use { channel ->
                channel.position(actualStartByte)
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
    }
}
