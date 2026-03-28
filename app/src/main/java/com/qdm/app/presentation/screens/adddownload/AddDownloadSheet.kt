package com.parveenbhadoo.qdm.presentation.screens.adddownload

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.parveenbhadoo.qdm.domain.model.AppSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parveenbhadoo.qdm.R
import com.parveenbhadoo.qdm.utils.FormatUtils

@Composable
fun AddDownloadSheet(
    initialUrl: String = "",
    initialReferer: String = "",
    initialCookies: String = "",
    initialUserAgent: String = "",
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit,
    onStarted: (String) -> Unit,
    viewModel: AddDownloadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) viewModel.setInitialUrl(initialUrl, initialReferer, initialCookies, initialUserAgent)
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onSavePathSelected(it.toString())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.add_download), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // URL input
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = viewModel::onUrlChanged,
                    label = { Text(stringResource(R.string.link_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                )

                // Save folder row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save to",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.savePath.isBlank()) "Downloads/QDM/{category}/"
                                   else uiState.savePath.substringAfterLast('/').ifBlank { uiState.savePath },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { folderLauncher.launch(null) }) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Change folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Filename + size — shown once fetched
                if (uiState.isFetched) {
                    OutlinedTextField(
                        value = uiState.fileName,
                        onValueChange = viewModel::onFileNameChanged,
                        label = { Text(stringResource(R.string.save_as)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = if (uiState.totalBytes > 0) {
                            {
                                Text(
                                    text = FormatUtils.formatBytes(uiState.totalBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        } else null
                    )
                }

                // Referer
                OutlinedTextField(
                    value = uiState.referer,
                    onValueChange = viewModel::onRefererChanged,
                    label = { Text(stringResource(R.string.referer)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.url.isNotBlank() && uiState.referer.isBlank()) {
                            IconButton(onClick = { viewModel.onRefererChanged(uiState.url.trim()) }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy URL as Referer",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )

                // Advanced toggle
                TextButton(
                    onClick = { viewModel.toggleAdvanced() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (uiState.showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Text(stringResource(R.string.advanced_options))
                }

                if (uiState.showAdvanced) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${stringResource(R.string.threads)}: ${uiState.threadCount}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = uiState.threadCount.toFloat(),
                            onValueChange = { viewModel.onThreadCountChanged(it.toInt()) },
                            valueRange = 1f..16f,
                            steps = 14
                        )
                        Text(
                            text = stringResource(R.string.user_agent),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.userAgent != AppSettings.DESKTOP_USER_AGENT,
                                onClick = { viewModel.onUserAgentChanged(AppSettings.MOBILE_USER_AGENT) },
                                label = { Text("Mobile") }
                            )
                            FilterChip(
                                selected = uiState.userAgent == AppSettings.DESKTOP_USER_AGENT,
                                onClick = { viewModel.onUserAgentChanged(AppSettings.DESKTOP_USER_AGENT) },
                                label = { Text("Desktop") }
                            )
                        }
                        OutlinedTextField(
                            value = uiState.cookies,
                            onValueChange = viewModel::onCookiesChanged,
                            label = { Text("Cookies") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                        Text(stringResource(R.string.authentication), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.username,
                                onValueChange = viewModel::onUsernameChanged,
                                label = { Text(stringResource(R.string.username)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.password,
                                onValueChange = viewModel::onPasswordChanged,
                                label = { Text(stringResource(R.string.password)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.addToQueue(onAdded) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.url.isNotBlank()
                    ) {
                        Text("Add")
                    }
                    Button(
                        onClick = { viewModel.addAndStart(onStarted) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.url.isNotBlank() && uiState.isFetched && !uiState.isLoading
                    ) {
                        Text("Start")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
