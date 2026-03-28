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
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Switch
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
                            text = { Text("Browser Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { menuExpanded = false; viewModel.showBrowserSettings() }
                        )
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
                LaunchedEffect(uiState.desktopMode) {
                    webViewInstance?.settings?.userAgentString = if (uiState.desktopMode)
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                    else
                        null // reset to default mobile UA
                    webViewInstance?.reload()
                }
                LaunchedEffect(uiState.javaScriptEnabled) {
                    webViewInstance?.settings?.javaScriptEnabled = uiState.javaScriptEnabled
                }
                LaunchedEffect(uiState.loadImages) {
                    webViewInstance?.settings?.loadsImagesAutomatically = uiState.loadImages
                }
                WebViewWrapper(
                    url = initialUrl,
                    adBlockHosts = if (uiState.adBlockEnabled) adBlockHosts else emptySet(),
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

    // Browser settings bottom sheet
    if (uiState.showBrowserSettings) {
        BrowserSettingsSheet(
            uiState = uiState,
            onDismiss = viewModel::dismissBrowserSettings,
            onToggleDesktopMode = viewModel::toggleDesktopMode,
            onToggleJavaScript = viewModel::toggleJavaScript,
            onToggleLoadImages = viewModel::toggleLoadImages,
            onToggleAdBlock = viewModel::toggleAdBlock,
            onClearCookies = {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                viewModel.dismissBrowserSettings()
            },
            onClearCache = {
                webViewInstance?.clearCache(true)
                viewModel.dismissBrowserSettings()
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserSettingsSheet(
    uiState: BrowserUiState,
    onDismiss: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleJavaScript: () -> Unit,
    onToggleLoadImages: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onClearCookies: () -> Unit,
    onClearCache: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Text(
            "Browser Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        BrowserSettingToggle(
            title = "Desktop Mode",
            subtitle = "Use desktop site layout",
            icon = Icons.Default.DesktopWindows,
            checked = uiState.desktopMode,
            onCheckedChange = { onToggleDesktopMode() }
        )
        BrowserSettingToggle(
            title = "JavaScript",
            subtitle = "Enable JavaScript on pages",
            checked = uiState.javaScriptEnabled,
            onCheckedChange = { onToggleJavaScript() }
        )
        BrowserSettingToggle(
            title = "Load Images",
            subtitle = "Show images on pages",
            checked = uiState.loadImages,
            onCheckedChange = { onToggleLoadImages() }
        )
        BrowserSettingToggle(
            title = "Ad Blocking",
            subtitle = "Block ads using host list",
            checked = uiState.adBlockEnabled,
            onCheckedChange = { onToggleAdBlock() }
        )
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearCookies)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Clear Cookies", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearCache)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Clear Cache", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.padding(bottom = 16.dp))
    }
}

@Composable
private fun BrowserSettingToggle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null,
                modifier = Modifier.padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
