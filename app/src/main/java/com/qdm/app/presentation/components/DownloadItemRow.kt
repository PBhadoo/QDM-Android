package com.parveenbhadoo.qdm.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.parveenbhadoo.qdm.R
import com.parveenbhadoo.qdm.domain.model.DownloadItem
import com.parveenbhadoo.qdm.domain.model.DownloadState
import com.parveenbhadoo.qdm.utils.FormatUtils
import com.parveenbhadoo.qdm.utils.MimeTypeHelper

private val ColorCompleted = Color(0xFF4CAF50)
private val ColorPaused = Color(0xFFFF9800)

@Composable
fun DownloadItemRow(
    item: DownloadItem,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRedownload: () -> Unit,
    onRedownloadWithOptions: () -> Unit,
    onRemove: () -> Unit,
    onOpenFolder: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyMoveRename: () -> Unit,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val borderColor = when (item.state) {
        is DownloadState.Completed -> ColorCompleted
        is DownloadState.Error -> MaterialTheme.colorScheme.error
        is DownloadState.Paused -> ColorPaused
        is DownloadState.Downloading, is DownloadState.Connecting -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val iconTint = if (borderColor == Color.Transparent) MaterialTheme.colorScheme.primary else borderColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(color = borderColor, size = Size(4.dp.toPx(), size.height))
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = mimeIcon(item.mimeType),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            ) {
                // Smart-truncated filename + resume badge on the same line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormatUtils.smartTruncate(item.fileName),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    ResumeBadge(supportsRanges = item.supportsRanges)
                }
                // Fixed-height size/progress row — prevents layout jump as text changes length
                Box(modifier = Modifier.height(16.dp).widthIn(min = 140.dp)) {
                    SizeProgressText(item)
                }
                Spacer(Modifier.height(2.dp))
                StateRow(item = item)
                // Chunk progress bar — only visible while actively downloading or paused
                if (item.state is DownloadState.Downloading || item.state is DownloadState.Paused) {
                    Spacer(Modifier.height(4.dp))
                    ChunkProgressBar(
                        progress = item.progress,
                        chunkCount = item.threadCount.coerceAtLeast(1),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                // Negative x offset aligns the right edge of the menu with the icon button
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    offset = DpOffset(x = (-160).dp, y = 0.dp)
                ) {
                    // State-dependent primary actions
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
                        is DownloadState.Pending -> {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_start)) },
                                onClick = { menuExpanded = false; onStart() }
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
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = { menuExpanded = false; onShare() }
                            )
                        }
                        else -> {}
                    }
                    HorizontalDivider()
                    // Universal actions available for all states
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_open_folder)) },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                        onClick = { menuExpanded = false; onOpenFolder() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_redownload)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = { menuExpanded = false; onRedownload() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_redownload_options)) },
                        onClick = { menuExpanded = false; onRedownloadWithOptions() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_copy_link)) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { menuExpanded = false; onCopyLink() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_copy_move_rename)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                        onClick = { menuExpanded = false; onCopyMoveRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_properties)) },
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        onClick = { menuExpanded = false; onProperties() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onRemove() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeBadge(supportsRanges: Boolean) {
    val (yesNo, color) = if (supportsRanges)
        stringResource(R.string.resume_yes) to ColorCompleted
    else
        stringResource(R.string.resume_no) to MaterialTheme.colorScheme.outline
    Text(
        text = "(Resume: $yesNo)",
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
private fun SizeProgressText(item: DownloadItem) {
    val text = when {
        item.state is DownloadState.Completed && item.totalBytes > 0 ->
            FormatUtils.formatBytes(item.totalBytes)
        item.totalBytes > 0 && item.downloadedBytes > 0 -> {
            val pct = (item.progress * 100).toInt()
            "${FormatUtils.formatBytes(item.downloadedBytes)} / ${FormatUtils.formatBytes(item.totalBytes)} ($pct%)"
        }
        item.totalBytes > 0 -> FormatUtils.formatBytes(item.totalBytes)
        item.downloadedBytes > 0 -> FormatUtils.formatBytes(item.downloadedBytes)
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
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
        is DownloadState.Connecting -> Text(
            text = stringResource(R.string.state_connecting),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        is DownloadState.Paused -> Text(
            text = stringResource(R.string.state_paused),
            style = MaterialTheme.typography.labelSmall,
            color = ColorPaused
        )
        is DownloadState.Completed -> Text(
            text = stringResource(R.string.state_completed),
            style = MaterialTheme.typography.labelSmall,
            color = ColorCompleted
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
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}
