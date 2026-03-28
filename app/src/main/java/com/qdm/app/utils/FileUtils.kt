package com.qdm.app.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import java.io.File

object FileUtils {

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(200).ifEmpty { "download" }
    }

    fun extractFileNameFromUrl(url: String): String {
        val path = url.substringBefore('?').substringAfterLast('/')
        return path.ifEmpty { "download" }
    }

    fun extractFileNameFromDisposition(disposition: String?): String? {
        if (disposition == null) return null
        val filenameRegex = Regex("""filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?""", RegexOption.IGNORE_CASE)
        return filenameRegex.find(disposition)?.groupValues?.getOrNull(1)?.trim()
    }

    // Returns the ParcelFileDescriptor — caller MUST close it (use .use {}) to avoid fd leak.
    fun openFileDescriptorForWrite(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "rw")
        } catch (e: Exception) {
            null
        }
    }

    fun createFileInDownloads(context: Context, fileName: String, mimeType: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            Uri.fromFile(File(dir, fileName))
        }
    }

    fun markFileDownloadComplete(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null)
        }
    }

    fun createDocumentFile(context: Context, treeUri: Uri, fileName: String, mimeType: String): Uri? {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        return dir.createFile(mimeType, fileName)?.uri
    }
}
