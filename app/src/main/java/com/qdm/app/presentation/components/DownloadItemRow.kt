package com.qdm.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qdm.app.R
import com.qdm.app.domain.model.DownloadItem
import com.qdm.app.domain.model.DownloadState
import com.qdm.app.utils.MimeTypeHelper

@Composable
fun DownloadItemRow(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRedownload: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = mimeIcon(item.mimeType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                StateRow(item = item)

                if (item.state is DownloadState.Downloading || item.state is DownloadState.Paused) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                when (item.state) {
                    is DownloadState.Downloading, is DownloadState.Connecting -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_pause)) },
                            onClick = { menuExpanded = false; onPause() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_cancel)) },
                            onClick = { menuExpanded = false; onCancel() }
                        )
                    }
                    is DownloadState.Paused, is DownloadState.Error -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_resume)) },
                            onClick = { menuExpanded = false; onResume() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_cancel)) },
                            onClick = { menuExpanded = false; onCancel() }
                        )
                    }
                    is DownloadState.Completed -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_open)) },
                            onClick = { menuExpanded = false; onOpen() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = { menuExpanded = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_redownload)) },
                            onClick = { menuExpanded = false; onRedownload() }
                        )
                    }
                    else -> {}
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_remove)) },
                    onClick = { menuExpanded = false; onRemove() }
                )
            }
        }
    }
}

@Composable
private fun StateRow(item: DownloadItem) {
    when (val state = item.state) {
        is DownloadState.Downloading -> SpeedEtaText(
            speedBps = state.speedBps,
            etaSeconds = state.etaSeconds,
            downloadedBytes = state.downloadedBytes,
            totalBytes = state.totalBytes
        )
        is DownloadState.Paused -> Text(
            text = stringResource(R.string.state_paused),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        is DownloadState.Completed -> Text(
            text = stringResource(R.string.state_completed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
        is DownloadState.Error -> Text(
            text = state.message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        is DownloadState.Scheduled -> Text(
            text = stringResource(R.string.state_scheduled),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Text(
            text = stringResource(R.string.state_pending),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun mimeIcon(mimeType: String) = when {
    MimeTypeHelper.isVideo(mimeType) -> Icons.Default.VideoFile
    MimeTypeHelper.isAudio(mimeType) -> Icons.Default.AudioFile
    MimeTypeHelper.isImage(mimeType) -> Icons.Default.Image
    MimeTypeHelper.isPdf(mimeType) -> Icons.Default.PictureAsPdf
    else -> Icons.Default.InsertDriveFile
}
