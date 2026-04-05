package com.parveenbhadoo.qdm.presentation.screens.browser

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parveenbhadoo.qdm.data.local.entity.BookmarkEntity
import com.parveenbhadoo.qdm.data.local.entity.BrowserHistoryEntity
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
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var urlInput by remember { mutableStateOf(TextFieldValue(initialUrl)) }
    var menuExpanded by remember { mutableStateOf(false) }
    var urlFocused by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<BrowserHistoryEntity>>(emptyList()) }

    // Download dialog state
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

    // Sync urlInput when active tab changes
    LaunchedEffect(uiState.activeTabIndex, uiState.currentUrl) {
        urlInput = TextFieldValue(uiState.currentUrl)
    }

    // Autocomplete suggestions
    LaunchedEffect(urlInput.text, urlFocused) {
        suggestions = if (urlFocused && urlInput.text.isNotBlank())
            viewModel.searchHistory(urlInput.text)
        else emptyList()
    }

    // --- Settings LaunchedEffects at top level, keyed on webViewInstance ---
    var desktopModeInit by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.desktopMode, webViewInstance) {
        val wv = webViewInstance ?: return@LaunchedEffect
        wv.settings.userAgentString = if (uiState.desktopMode)
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        else null
        if (desktopModeInit) wv.reload()
        desktopModeInit = true
    }
    LaunchedEffect(uiState.javaScriptEnabled, webViewInstance) {
        webViewInstance?.settings?.javaScriptEnabled = uiState.javaScriptEnabled
    }
    LaunchedEffect(uiState.loadImages, webViewInstance) {
        webViewInstance?.settings?.loadsImagesAutomatically = uiState.loadImages
    }

    // Show snackbar when media is detected — keyed on the URL so it fires exactly once per detection
    LaunchedEffect(uiState.detectedMediaUrl) {
        val detectedUrl = uiState.detectedMediaUrl ?: return@LaunchedEffect
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

    // Tab panel drawer state
    val tabDrawerState = rememberDrawerState(DrawerValue.Closed)
    LaunchedEffect(uiState.showTabPanel) {
        if (uiState.showTabPanel) tabDrawerState.open() else tabDrawerState.close()
    }
    LaunchedEffect(tabDrawerState.currentValue) {
        if (tabDrawerState.currentValue == DrawerValue.Closed && uiState.showTabPanel) {
            viewModel.dismissTabPanel()
        }
    }

    // When switching tabs, load the new tab's URL in the WebView
    var prevTabIndex by remember { mutableStateOf(uiState.activeTabIndex) }
    LaunchedEffect(uiState.activeTabIndex) {
        if (uiState.activeTabIndex != prevTabIndex) {
            prevTabIndex = uiState.activeTabIndex
            val tabUrl = uiState.tabs.getOrNull(uiState.activeTabIndex)?.url ?: return@LaunchedEffect
            webViewInstance?.loadUrl(tabUrl)
            urlInput = TextFieldValue(tabUrl)
        }
    }

    ModalNavigationDrawer(
        drawerState = tabDrawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.85f)) {
                TabPanelContent(
                    tabs = uiState.tabs,
                    activeIndex = uiState.activeTabIndex,
                    onSwitchTab = { i ->
                        viewModel.switchTab(i)
                        viewModel.dismissTabPanel()
                    },
                    onCloseTab = { i -> viewModel.closeTab(i) },
                    onNewTab = {
                        viewModel.openNewTab()
                        viewModel.dismissTabPanel()
                        webViewInstance?.loadUrl("https://www.google.com")
                    },
                    onClose = { viewModel.dismissTabPanel() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // [1] Tab count button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.showTabPanel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${uiState.tabs.size}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        // [2] URL input with inline reload button
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 48.dp)
                                .onFocusChanged { focusState ->
                                    urlFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        urlInput = urlInput.copy(
                                            selection = TextRange(0, urlInput.text.length)
                                        )
                                    } else {
                                        suggestions = emptyList()
                                    }
                                },
                            placeholder = {
                                Text("Search or enter URL", style = MaterialTheme.typography.bodyMedium)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    val resolved = resolveInput(urlInput.text)
                                    viewModel.setUrl(resolved)
                                    webViewInstance?.loadUrl(resolved)
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { webViewInstance?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                                }
                            }
                        )

                        // [3] Download detection button — immediately left of 3-dots
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                val dl = uiState.detectedMediaUrl
                                if (dl != null) {
                                    val cookies = android.webkit.CookieManager.getInstance().getCookie(uiState.currentUrl) ?: ""
                                    triggerDownload(dl, uiState.detectedMediaHeaders, cookies)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = if (uiState.detectedMediaUrl != null)
                                    "Download detected media"
                                else
                                    "No downloads detected",
                                tint = if (uiState.detectedMediaUrl != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // [4] 3-dots menu
                        Box {
                            IconButton(
                                modifier = Modifier.size(40.dp),
                                onClick = { menuExpanded = true }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                // Compact icon row: back · forward · star · reload · settings
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconButton(
                                        enabled = uiState.canGoBack,
                                        onClick = { menuExpanded = false; webViewInstance?.goBack() }
                                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                                    IconButton(
                                        enabled = uiState.canGoForward,
                                        onClick = { menuExpanded = false; webViewInstance?.goForward() }
                                    ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward") }
                                    IconButton(onClick = { menuExpanded = false; viewModel.toggleBookmark() }) {
                                        Icon(
                                            if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(onClick = { menuExpanded = false; webViewInstance?.reload() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                                    }
                                    IconButton(onClick = { menuExpanded = false; viewModel.showBrowserSettings() }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("History") },
                                    onClick = { menuExpanded = false; viewModel.showHistory() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bookmarks") },
                                    onClick = { menuExpanded = false; viewModel.showBookmarks() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ad Block Manager") },
                                    onClick = { menuExpanded = false; viewModel.showAdBlockManager() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Browser Info") },
                                    onClick = { menuExpanded = false; viewModel.showBrowserInfo() }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Back to QDM") },
                                    onClick = { menuExpanded = false; onNavigateBack() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Exit Browser", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.closeAllTabs()
                                        onNavigateBack()
                                    }
                                )
                            }
                        }
                    }
                }
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
                        adBlockHosts = if (uiState.adBlockEnabled) adBlockHosts else emptySet(),
                        onMediaDetected = { url, headers -> viewModel.onMediaDetected(url, headers) },
                        onPageTitleChanged = { title -> viewModel.onPageFinished(uiState.currentUrl, title) },
                        onProgressChanged = viewModel::onProgressChanged,
                        onPageStarted = { url -> viewModel.onPageStarted(url); urlInput = TextFieldValue(url) },
                        onPageFinished = { url -> viewModel.onPageFinished(url, uiState.pageTitle); urlInput = TextFieldValue(url) },
                        onNavigationState = viewModel::onNavigationStateChanged,
                        onAdBlocked = viewModel::onAdBlocked,
                        onDownloadRequested = { url, headers, cookies ->
                            triggerDownload(url, headers, cookies)
                        },
                        webViewRef = { webViewInstance = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Autocomplete suggestions overlay — shown below the toolbar
                if (urlFocused && suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .padding(horizontal = 8.dp)
                            .heightIn(max = 280.dp)
                    ) {
                        LazyColumn {
                            items(suggestions, key = { it.id }) { entry ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val url = entry.url
                                            urlInput = TextFieldValue(url)
                                            viewModel.setUrl(url)
                                            webViewInstance?.loadUrl(url)
                                            urlFocused = false
                                            suggestions = emptyList()
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
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    // Download dialog
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

    // Browser settings sheet
    if (uiState.showBrowserSettings) {
        BrowserSettingsSheet(
            uiState = uiState,
            onDismiss = viewModel::dismissBrowserSettings,
            onToggleDesktopMode = viewModel::toggleDesktopMode,
            onToggleJavaScript = viewModel::toggleJavaScript,
            onToggleLoadImages = viewModel::toggleLoadImages,
            onToggleAdBlock = viewModel::toggleAdBlock,
            onOpenAdBlockManager = { viewModel.dismissBrowserSettings(); viewModel.showAdBlockManager() },
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

    // Ad Block Manager sheet
    if (uiState.showAdBlockManager) {
        AdBlockManagerSheet(
            uiState = uiState,
            onDismiss = viewModel::dismissAdBlockManager,
            onToggleAdBlock = viewModel::toggleAdBlock,
            onToggleSource = viewModel::toggleFilterSource,
            onUpdateSource = viewModel::updateFilterSource,
            onUpdateAll = viewModel::updateAllFilterSources,
            onSetAllSources = viewModel::setAllFilterSources
        )
    }

    // Browser Info sheet
    if (uiState.showBrowserInfo) {
        BrowserCapabilitiesSheet(onDismiss = viewModel::dismissBrowserInfo)
    }

    // Bookmarks sheet
    if (uiState.showBookmarks) {
        BookmarksSheet(
            bookmarks = bookmarks,
            onDismiss = viewModel::dismissBookmarks,
            onOpen = { url ->
                urlInput = TextFieldValue(url)
                viewModel.setUrl(url)
                webViewInstance?.loadUrl(url)
                viewModel.dismissBookmarks()
            },
            onDelete = viewModel::deleteBookmark
        )
    }

    // History sheet
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
                                    urlInput = TextFieldValue(url)
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

// ---- Tab Panel ----

@Composable
private fun TabPanelContent(
    tabs: List<BrowserTab>,
    activeIndex: Int,
    onSwitchTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tabs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onNewTab) {
                Icon(Icons.Default.Add, contentDescription = "New Tab")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close panel")
            }
        }
        HorizontalDivider()
        LazyColumn {
            itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (index == activeIndex) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { onSwitchTab(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tab.title.ifBlank { "New Tab" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tab.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onCloseTab(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close tab",
                            modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// ---- Bookmarks Sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bookmarks", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No bookmarks yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(bookmarks, key = { it.id }) { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(bookmark.url) }
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookmark.title.ifBlank { bookmark.url },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = bookmark.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onDelete(bookmark.url) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete bookmark",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ---- Browser Settings Sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserSettingsSheet(
    uiState: BrowserUiState,
    onDismiss: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleJavaScript: () -> Unit,
    onToggleLoadImages: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onOpenAdBlockManager: () -> Unit,
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
            subtitle = "Block ads (${uiState.adBlockHostCount} hosts from ${uiState.filterSources.count { it.enabled }} sources)",
            checked = uiState.adBlockEnabled,
            onCheckedChange = { onToggleAdBlock() }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAdBlockManager)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                "Manage Ad Block Filters",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearCookies)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Clear Cookies", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearCache)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Clear Cache", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
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
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ---- Ad Block Manager Sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdBlockManagerSheet(
    uiState: BrowserUiState,
    onDismiss: () -> Unit,
    onToggleAdBlock: () -> Unit,
    onToggleSource: (String) -> Unit,
    onUpdateSource: (String) -> Unit,
    onUpdateAll: () -> Unit,
    onSetAllSources: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ad Block Manager", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = uiState.adBlockEnabled, onCheckedChange = { onToggleAdBlock() })
        }
        HorizontalDivider()
        // Summary + Enable/Disable All + Update All
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val anyEnabled = uiState.filterSources.any { it.enabled }
            val anyUpdating = uiState.filterSources.any { it.isUpdating }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${uiState.adBlockHostCount} hosts from ${uiState.filterSources.count { it.enabled }} sources",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (anyUpdating) {
                    Text("Updating…", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            TextButton(onClick = { onSetAllSources(!anyEnabled) }) {
                Text(if (anyEnabled) "Disable All" else "Enable All")
            }
            TextButton(
                onClick = onUpdateAll,
                enabled = !anyUpdating
            ) {
                Text("Update All")
            }
        }
        HorizontalDivider()

        // Source list
        LazyColumn {
            items(uiState.filterSources, key = { it.source.id }) { sourceState ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onToggleSource(sourceState.source.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (sourceState.enabled) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (sourceState.enabled) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sourceState.source.name, style = MaterialTheme.typography.bodyMedium)
                            val meta = buildString {
                                if (sourceState.hostCount > 0) append("${sourceState.hostCount} hosts")
                                if (sourceState.lastUpdated > 0L) {
                                    if (isNotBlank()) append(" · ")
                                    append(SimpleDateFormat("dd MMM yy", Locale.getDefault())
                                        .format(Date(sourceState.lastUpdated)))
                                } else if (sourceState.hostCount == 0) {
                                    append("Not downloaded")
                                }
                            }
                            Text(
                                meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (sourceState.error != null) {
                                Text(
                                    "Error: ${sourceState.error}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (sourceState.isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(
                                onClick = { onUpdateSource(sourceState.source.id) },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Update", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ---- Browser Capabilities Sheet ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserCapabilitiesSheet(onDismiss: () -> Unit) {
    val capabilities = remember {
        listOf(
            "HTML5 / CSS3" to "✓ Full support",
            "JavaScript" to "✓ Toggle available",
            "DOM Storage / IndexedDB" to "✓ Supported",
            "Cookies" to "✓ Managed automatically",
            "Media (video / audio)" to "✓ HTML5 playback",
            "File Downloads" to "✓ Intercepted by QDM",
            "Ad Blocking" to "✓ Host-based + ABP patterns",
            "Multi-Tab Browsing" to "✓ Up to ~8 tabs recommended",
            "Bookmarks" to "✓ Supported",
            "Browsing History" to "✓ Saved to local DB",
            "Custom User Agent" to "✓ Supported",
            "Desktop Mode" to "✓ Desktop UA on toggle",
            "WebRTC (camera / mic)" to "✓ Needs permissions",
            "Geolocation" to "✓ Needs permissions",
            "Full-Screen Video" to "✓ Supported",
            "Service Workers" to "✓ Android API 24+",
            "Hardware Acceleration" to "✓ Enabled",
            "PDF Viewing" to "✗ Opens external viewer",
            "Browser Extensions" to "✗ Not supported",
            "Password Autofill" to "✗ No native support",
            "Pop-up Windows" to "✗ Blocked by default"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Text(
            "Browser Capabilities",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        LazyColumn {
            items(capabilities) { (feature, status) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(feature, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.startsWith("✓")) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(Modifier.height(32.dp)) }
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
