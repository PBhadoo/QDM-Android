package com.parveenbhadoo.qdm.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parveenbhadoo.qdm.domain.model.DownloadItem
import com.parveenbhadoo.qdm.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadPropertiesDialog(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties") },
        text = {
            LazyColumn {
                item { PropRow("File name", item.fileName) }
                item { PropRow("URL", item.url) }
                item { PropRow("Size", if (item.totalBytes > 0) FormatUtils.formatBytes(item.totalBytes) else "Unknown") }
                item { PropRow("Downloaded", FormatUtils.formatBytes(item.downloadedBytes)) }
                item { PropRow("MIME type", item.mimeType.ifBlank { "Unknown" }) }
                item { PropRow("Save path", item.savePath.ifBlank { "Default" }) }
                item { PropRow("Threads", item.threadCount.toString()) }
                item { PropRow("Resumable", if (item.supportsRanges) "Yes" else "No") }
                item { PropRow("Status", item.state::class.simpleName ?: "Unknown") }
                item { PropRow("Added", dateFormat.format(Date(item.addedAt))) }
                if (item.completedAt != null) {
                    item { PropRow("Completed", dateFormat.format(Date(item.completedAt))) }
                }
                if (item.errorMessage != null) {
                    item { PropRow("Error", item.errorMessage) }
                }
                if (!item.referer.isNullOrBlank()) {
                    item { PropRow("Referer", item.referer) }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { onShare(); onDismiss() }) { Text("Share") }
                TextButton(onClick = { onOpen(); onDismiss() }) { Text("Open") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun PropRow(label: String, value: String) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider()
}
