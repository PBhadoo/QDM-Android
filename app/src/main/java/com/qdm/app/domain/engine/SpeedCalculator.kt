package com.qdm.app.domain.engine

class SpeedCalculator {
    private data class Sample(val timestamp: Long, val bytes: Long)

    private val samples = ArrayDeque<Sample>()
    private val windowMs = 3000L

    fun record(bytesReceived: Long) {
        val now = System.currentTimeMillis()
        samples.addLast(Sample(now, bytesReceived))
        // Remove samples older than the window
        while (samples.isNotEmpty() && now - samples.first().timestamp > windowMs) {
            samples.removeFirst()
        }
    }

    fun speedBps(): Long {
        if (samples.size < 2) return 0L
        val windowActual = (samples.last().timestamp - samples.first().timestamp).coerceAtLeast(1L)
        val totalBytes = samples.sumOf { it.bytes }
        return (totalBytes * 1000L) / windowActual
    }

    fun etaSeconds(remainingBytes: Long): Long {
        val speed = speedBps()
        if (speed <= 0 || remainingBytes <= 0) return 0L
        return remainingBytes / speed
    }

    fun reset() = samples.clear()
}
