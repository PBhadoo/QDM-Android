package com.qdm.app.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qdm.app.utils.FormatUtils

@Composable
fun SpeedEtaText(
    speedBps: Long,
    etaSeconds: Long,
    downloadedBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val speed = FormatUtils.formatSpeed(speedBps)
    val eta = FormatUtils.formatEta(etaSeconds)
    val sizeText = if (totalBytes > 0)
        "${FormatUtils.formatBytes(downloadedBytes)} / ${FormatUtils.formatBytes(totalBytes)}"
    else FormatUtils.formatBytes(downloadedBytes)

    Text(
        text = if (speedBps > 0) "$speed — $eta  ·  $sizeText" else sizeText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
