package com.parveenbhadoo.qdm.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * A segmented progress bar that shows N equal-width segments — one per download thread.
 * Segments fill left-to-right based on overall progress, giving a visual sense of parallel work.
 */
@Composable
fun ChunkProgressBar(
    progress: Float,
    chunkCount: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val count = chunkCount.coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        val gapPx = if (count > 1) 2.dp.toPx() else 0f
        val segW = (size.width - gapPx * (count - 1)) / count

        repeat(count) { i ->
            val x = i * (segW + gapPx)
            // How much of this segment is filled
            val segProgress = ((progress * count) - i).coerceIn(0f, 1f)
            // Background
            drawRect(
                color = bg,
                topLeft = Offset(x, 0f),
                size = Size(segW, size.height)
            )
            // Fill
            if (segProgress > 0f) {
                drawRect(
                    color = primary,
                    topLeft = Offset(x, 0f),
                    size = Size(segW * segProgress, size.height)
                )
            }
        }
    }
}
