package com.parveenbhadoo.qdm.utils

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

    /**
     * Truncates a filename so the start and extension are preserved.
     * "Argylle 2024 HDTS 720p Hindi + English x264 AAC.mkv" → "Argylle 2024....mkv"
     * Short names that already fit within [maxLen] are returned unchanged.
     */
    fun smartTruncate(name: String, maxLen: Int = 22): String {
        val dotIdx = name.lastIndexOf('.')
        if (dotIdx <= 0) {
            // No extension — just truncate with ellipsis
            return if (name.length <= maxLen) name else "${name.take(maxLen - 3)}..."
        }
        val base = name.substring(0, dotIdx)
        val ext = name.substring(dotIdx) // includes the dot, e.g. ".mkv"
        if (name.length <= maxLen) return name
        // Keep as many chars of the base as possible, then .... then ext
        val ellipsis = "...."
        val available = maxLen - ext.length - ellipsis.length
        return if (available > 0) "${base.take(available)}$ellipsis$ext" else "$ellipsis$ext"
    }
}
