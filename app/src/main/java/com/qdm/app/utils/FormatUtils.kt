package com.qdm.app.utils

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "?"
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun formatSpeed(bps: Long): String {
        if (bps <= 0) return "—"
        return "${formatBytes(bps)}/s"
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0) return "—"
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }

    fun formatProgress(downloaded: Long, total: Long): String {
        val pct = if (total > 0) (downloaded * 100L / total) else 0
        return "${formatBytes(downloaded)} / ${formatBytes(total)} ($pct%)"
    }
}
