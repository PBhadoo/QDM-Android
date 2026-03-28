package com.qdm.app.domain.usecase

import com.qdm.app.domain.model.FileMetadata
import com.qdm.app.utils.FileUtils
import com.qdm.app.utils.MimeTypeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class FetchFileMetadataUseCase @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun execute(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Result<FileMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> requestBuilder.header(k, v) }

            // Try HEAD first
            val headResponse = client.newCall(requestBuilder.head().build()).execute()
            if (headResponse.code == 405) {
                headResponse.close()
                // Server doesn't support HEAD — use GET with Range: bytes=0-0
                return@runCatching fetchWithGet(url, headers)
            }

            headResponse.use { resp ->
                val totalBytes = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                val contentDisposition = resp.header("Content-Disposition")
                val contentType = resp.header("Content-Type")
                val acceptRanges = resp.header("Accept-Ranges")?.lowercase()
                val etag = resp.header("ETag")
                val lastModified = resp.header("Last-Modified")

                val fileName = FileUtils.extractFileNameFromDisposition(contentDisposition)
                    ?: FileUtils.extractFileNameFromUrl(url)
                val mimeType = MimeTypeHelper.fromContentType(contentType)
                val supportsRanges = acceptRanges != null && acceptRanges != "none"

                FileMetadata(
                    totalBytes = totalBytes,
                    fileName = FileUtils.sanitizeFileName(fileName),
                    mimeType = mimeType,
                    supportsRanges = supportsRanges,
                    etag = etag,
                    lastModified = lastModified
                )
            }
        }
    }

    private fun fetchWithGet(url: String, headers: Map<String, String>): FileMetadata {
        val requestBuilder = Request.Builder().url(url)
            .header("Range", "bytes=0-0")
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        val response = client.newCall(requestBuilder.build()).execute()
        return response.use { resp ->
            val contentRange = resp.header("Content-Range") // e.g. "bytes 0-0/12345"
            val totalBytes = contentRange?.substringAfterLast('/')?.toLongOrNull()
                ?: resp.header("Content-Length")?.toLongOrNull()
                ?: -1L
            val contentType = resp.header("Content-Type")
            val contentDisposition = resp.header("Content-Disposition")

            val fileName = FileUtils.extractFileNameFromDisposition(contentDisposition)
                ?: FileUtils.extractFileNameFromUrl(url)

            FileMetadata(
                totalBytes = totalBytes,
                fileName = FileUtils.sanitizeFileName(fileName),
                mimeType = MimeTypeHelper.fromContentType(contentType),
                supportsRanges = resp.code == 206,
                etag = resp.header("ETag"),
                lastModified = resp.header("Last-Modified")
            )
        }
    }
}
