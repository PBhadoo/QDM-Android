package com.parveenbhadoo.qdm.presentation.screens.browser

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parveenbhadoo.qdm.presentation.screens.adddownload.AddDownloadSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    initialUrl: String,
    onNavigateBack: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adBlockHosts by viewModel.adBlockHosts.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var urlInput by remember { mutableStateOf(initialUrl) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Download dialog state — local to BrowserScreen, dialog shows over the browser
    var downloadDialogUrl by remember { mutableStateOf("") }
    var downloadDialogReferer by remember { mutableStateOf("") }
    var downloadDialogCookies by remember { mutableStateOf("") }
    var downloadDialogUserAgent by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }

    fun triggerDownload(url: String, headers: Map<String, String>, cookies: String) {
        viewModel.dismissMedia()
        downloadDialogUrl = url
        downloadDialogReferer = headers["Referer"] ?: uiState.currentUrl
        downloadDialogCookies = cookies
        downloadDialogUserAgent = headers["User-Agent"] ?: ""
        showDownloadDialog = true
    }

    // Show snackbar when media is detected
    val detectedUrl = uiState.detectedMediaUrl
    if (detectedUrl != null) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Media detected: ${detectedUrl.substringAfterLast('/').take(40)}",
                actionLabel = "Download"
            )
            if (result == SnackbarResult.ActionPerformed) {
                val cookies = android.webkit.CookieManager.getInstance().getCookie(uiState.currentUrl) ?: ""
                triggerDownload(detectedUrl, uiState.detectedMediaHeaders, cookies)
            }
            viewModel.dismissMedia()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Search or enter URL", style = MaterialTheme.typography.bodySmall)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val resolved = resolveInput(urlInput)
                                viewModel.setUrl(resolved)
                                webViewInstance?.loadUrl(resolved)
                            }
                        )
                    )
                },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Back") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) },
                            enabled = uiState.canGoBack,
                            onClick = { menuExpanded = false; webViewInstance?.goBack() }
                        )
                        DropdownMenuItem(
                            text = { Text("Forward") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                            enabled = uiState.canGoForward,
                            onClick = { menuExpanded = false; webViewInstance?.goForward() }
                        )
                        DropdownMenuItem(
                            text = { Text("Reload") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            onClick = { menuExpanded = false; webViewInstance?.reload() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = { menuExpanded = false; viewModel.showHistory() }
                        )
                        DropdownMenuItem(
                            text = { Text("Back to QDM") },
                            onClick = { menuExpanded = false; onNavigateBack() }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { uiState.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WebViewWrapper(
                    url = initialUrl,
                    adBlockHosts = adBlockHosts,
                    onMediaDetected = { url, headers -> viewModel.onMediaDetected(url, headers) },
                    onPageTitleChanged = { title -> viewModel.onPageFinished(uiState.currentUrl, title) },
                    onProgressChanged = viewModel::onProgressChanged,
                    onPageStarted = { url -> viewModel.onPageStarted(url); urlInput = url },
                    onPageFinished = { url -> viewModel.onPageFinished(url, uiState.pageTitle); urlInput = url },
                    onNavigationState = viewModel::onNavigationStateChanged,
                    onAdBlocked = viewModel::onAdBlocked,
                    onDownloadRequested = { url, headers, cookies ->
                        triggerDownload(url, headers, cookies)
                    },
                    webViewRef = { webViewInstance = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Download dialog — shown over the browser, browser stays open
    if (showDownloadDialog) {
        AddDownloadSheet(
            initialUrl = downloadDialogUrl,
            initialReferer = downloadDialogReferer,
            initialCookies = downloadDialogCookies,
            initialUserAgent = downloadDialogUserAgent,
            onDismiss = { showDownloadDialog = false },
            onAdded = { showDownloadDialog = false },
            onStarted = { showDownloadDialog = false }
        )
    }

    // History bottom sheet
    if (uiState.showHistory) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissHistory() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("History", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear all")
                }
            }
            HorizontalDivider()
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(history, key = { it.id }) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val url = entry.url
                                    urlInput = url
                                    viewModel.setUrl(url)
                                    webViewInstance?.loadUrl(url)
                                    viewModel.dismissHistory()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = entry.title.ifBlank { entry.url },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = entry.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                    .format(Date(entry.visitedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

private fun resolveInput(input: String): String {
    val trimmed = input.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val looksLikeDomain = !trimmed.contains(' ') &&
        trimmed.contains('.') &&
        !trimmed.startsWith('/')
    return if (looksLikeDomain) "https://$trimmed"
    else "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
}
