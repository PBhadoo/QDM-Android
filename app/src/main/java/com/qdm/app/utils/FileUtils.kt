package com.parveenbhadoo.qdm.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File

object FileUtils {

    fun sanitizeFileName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(200).ifEmpty { "download" }

    fun extractFileNameFromUrl(url: String): String {
        val path = url.substringBefore('?').substringAfterLast('/')
        return path.ifEmpty { "download" }
    }

    fun extractFileNameFromDisposition(disposition: String?): String? {
        if (disposition == null) return null
        val filenameRegex = Regex("""filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?""", RegexOption.IGNORE_CASE)
        return filenameRegex.find(disposition)?.groupValues?.getOrNull(1)?.trim()
    }

    /** Category subfolder name derived from MIME type, used under Downloads/QDM/. */
    fun categoryFolder(mimeType: String): String = when {
        mimeType.startsWith("video/") -> "Videos"
        mimeType.startsWith("audio/") -> "Music"
        mimeType.startsWith("image/") -> "Images"
        mimeType == "application/pdf"
            || mimeType.startsWith("text/")
            || mimeType.startsWith("application/msword")
            || mimeType.startsWith("application/vnd.") -> "Documents"
        else -> "Other"
    }

    /**
     * Creates the output file under Downloads/QDM/{category}/ using MediaStore (API 29+)
     * or a plain File on older devices.
     * Returns the URI to write to, or null on failure.
     */
    fun createFileForDownload(context: Context, fileName: String, mimeType: String): Uri? {
        val category = categoryFolder(mimeType)
        val safeMime = mimeType.ifBlank { "application/octet-stream" }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, safeMime)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/QDM/$category/")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            QdmLog.d("FileUtils", "MediaStore insert → $uri  (file=$fileName, cat=$category)")
            uri
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "QDM/$category"
            )
            dir.mkdirs()
            val file = File(dir, fileName)
            val uri = Uri.fromFile(file)
            QdmLog.d("FileUtils", "Legacy file → $uri  (file=$fileName, cat=$category)")
            uri
        }
    }

    /**
     * Creates a child document inside a SAF tree URI (folder selected via DocumentTree picker).
     */
    fun createDocumentFile(context: Context, treeUri: Uri, fileName: String, mimeType: String): Uri? {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: run {
            QdmLog.e("FileUtils", "fromTreeUri returned null for $treeUri")
            return null
        }
        val uri = dir.createFile(mimeType.ifBlank { "application/octet-stream" }, fileName)?.uri
        QdmLog.d("FileUtils", "SAF createFile → $uri  (tree=$treeUri)")
        return uri
    }

    /**
     * Opens a ParcelFileDescriptor for writing. Caller MUST close it (use .use{}) to avoid fd leak.
     * Returns null and logs on failure.
     */
    fun openFileDescriptorForWrite(context: Context, uri: Uri): ParcelFileDescriptor? {
        // Try "rw" (needed for resume/seek). If that fails, try plain "w".
        return try {
            context.contentResolver.openFileDescriptor(uri, "rw")
                ?.also { QdmLog.d("FileUtils", "Opened rw fd for $uri") }
        } catch (e: Exception) {
            QdmLog.w("FileUtils", "rw failed for $uri (${e.message}), retrying with w")
            try {
                context.contentResolver.openFileDescriptor(uri, "w")
                    ?.also { QdmLog.d("FileUtils", "Opened w fd for $uri") }
            } catch (e2: Exception) {
                QdmLog.e("FileUtils", "Cannot open $uri for writing: ${e2.message}", e2)
                null
            }
        }
    }

    fun markFileDownloadComplete(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(uri, values, null, null)
                QdmLog.d("FileUtils", "Marked complete: $uri")
            } catch (e: Exception) {
                QdmLog.w("FileUtils", "markComplete failed for $uri: ${e.message}")
            }
        }
    }
}
