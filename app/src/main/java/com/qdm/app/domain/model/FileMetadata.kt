package com.parveenbhadoo.qdm.domain.model

data class FileMetadata(
    val totalBytes: Long,
    val fileName: String,
    val mimeType: String,
    val supportsRanges: Boolean,
    val etag: String?,
    val lastModified: String?
)
