package com.parveenbhadoo.qdm.utils

import android.webkit.MimeTypeMap

object MimeTypeHelper {
    fun fromUrl(url: String): String {
        val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    fun fromContentType(contentType: String?): String {
        if (contentType == null) return "application/octet-stream"
        return contentType.split(";").first().trim().ifEmpty { "application/octet-stream" }
    }

    fun isVideo(mimeType: String) = mimeType.startsWith("video/")
    fun isAudio(mimeType: String) = mimeType.startsWith("audio/")
    fun isImage(mimeType: String) = mimeType.startsWith("image/")
    fun isPdf(mimeType: String) = mimeType == "application/pdf"
    fun isArchive(mimeType: String) = mimeType in setOf(
        "application/zip", "application/x-rar-compressed",
        "application/x-7z-compressed", "application/gzip"
    )
    fun isApk(mimeType: String) = mimeType == "application/vnd.android.package-archive"
    fun isDocument(mimeType: String) = mimeType.startsWith("text/") ||
            mimeType.contains("document") || mimeType.contains("spreadsheet")
}
