package com.qdm.app.presentation.screens.adddownload

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdm.app.R
import com.qdm.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDownloadSheet(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onDownloadStarted: (String) -> Unit,
    viewModel: AddDownloadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) viewModel.setInitialUrl(initialUrl)
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.add_download), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChanged,
                label = { Text(stringResource(R.string.link_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            OutlinedTextField(
                value = uiState.referer,
                onValueChange = viewModel::onRefererChanged,
                label = { Text(stringResource(R.string.referer)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.savePath.substringAfterLast('/').ifBlank { uiState.savePath },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.choose_folder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { folderLauncher.launch(null) }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.choose_folder))
                }
            }

            if (!uiState.isFetched) {
                Button(
                    onClick = { viewModel.fetchInfo() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.url.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text(stringResource(R.string.fetch_info))
                }
            }

            if (uiState.isFetched || uiState.error != null) {
                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                }

                if (uiState.isFetched) {
                    OutlinedTextField(
                        value = uiState.fileName,
                        onValueChange = viewModel::onFileNameChanged,
                        label = { Text(stringResource(R.string.save_as)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (uiState.totalBytes > 0) {
                        Text(
                            text = FormatUtils.formatBytes(uiState.totalBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                        OutlinedTextField(
                            value = uiState.userAgent,
                            onValueChange = viewModel::onUserAgentChanged,
                            label = { Text(stringResource(R.string.user_agent)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                        OutlinedTextField(
                            value = uiState.cookies,
                            onValueChange = viewModel::onCookiesChanged,
                            label = { Text("Cookies") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                        // Authentication
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

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.startDownload(onDownloadStarted) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isFetched && uiState.fileName.isNotBlank()
                ) {
                    Text(stringResource(R.string.download))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
