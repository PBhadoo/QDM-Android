package com.parveenbhadoo.qdm.presentation.screens.main

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parveenbhadoo.qdm.R
import com.parveenbhadoo.qdm.presentation.components.DownloadItemRow
import com.parveenbhadoo.qdm.presentation.components.DownloadPropertiesDialog
import com.parveenbhadoo.qdm.presentation.components.MultiFab
import com.parveenbhadoo.qdm.presentation.screens.adddownload.AddDownloadSheet
import com.parveenbhadoo.qdm.utils.NetworkUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToBrowser: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredDownloads by remember { derivedStateOf { uiState.filteredDownloads } }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var mainMenuExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = uiState.activeTab.ordinal,
        pageCount = { DownloadTab.values().size }
    )

    // File picker for Import from Text File
    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: return@launch
                val urls = content
                    .split(Regex("[,\n]+"))
                    .map { it.trim() }
                    .filter { it.startsWith("http://") || it.startsWith("https://") }
                if (urls.isEmpty()) return@launch
                // First URL: show AddDownloadSheet so user can confirm/edit and start
                viewModel.showAddSheet(urls.first())
                // Remaining URLs: add directly to queue (auto-start when slot opens)
                if (urls.size > 1) {
                    urls.drop(1).forEach { viewModel.queueBulkUrl(it) }
                }
            } catch (e: Exception) {
                // Silent fail — file read error
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onFolderSetupCustomPicked(it.toString())
        }
    }

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) focusRequester.requestFocus()
    }

    // Pager swipe → update ViewModel tab
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            viewModel.selectTab(DownloadTab.values()[pagerState.currentPage])
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // 80% width, full settings page inside the drawer
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.80f)) {
                DrawerContent(
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Crossfade(targetState = uiState.isSearchActive, label = "search_title") { active ->
                                if (active) {
                                    OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = viewModel::onSearchQueryChanged,
                                        placeholder = { Text("Search downloads…") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                        trailingIcon = {
                                            IconButton(onClick = { focusManager.clearFocus(); viewModel.toggleSearch() }) {
                                                Icon(Icons.Default.Close, contentDescription = "Close search")
                                            }
                                        }
                                    )
                                } else {
                                    Text(stringResource(R.string.app_name))
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (uiState.isSearchActive) {
                            IconButton(onClick = { focusManager.clearFocus(); viewModel.toggleSearch() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        if (!uiState.isSearchActive) {
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { onNavigateToBrowser("https://www.google.com") }) {
                                Icon(Icons.Outlined.Language, contentDescription = "Browser")
                            }
                            Box {
                                IconButton(onClick = { mainMenuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(
                                    expanded = mainMenuExpanded,
                                    onDismissRequest = { mainMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.70f),
                                    offset = DpOffset(x = (-8).dp, y = 0.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sort by date") },
                                        onClick = { mainMenuExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by name") },
                                        onClick = { mainMenuExpanded = false }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Clear completed") },
                                        onClick = {
                                            mainMenuExpanded = false
                                            uiState.downloads
                                                .filter { it.state is com.parveenbhadoo.qdm.domain.model.DownloadState.Completed }
                                                .forEach { viewModel.onRemove(it.id) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!uiState.isSearchActive) {
                    MultiFab(
                        onAddLink = { viewModel.showAddSheet() },
                        onPasteClipboard = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            val url = NetworkUtils.extractUrl(text) ?: ""
                            viewModel.showAddSheet(url)
                        },
                        onImportFromFile = {
                            importFileLauncher.launch(arrayOf("text/plain", "text/*"))
                        }
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                ScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 0.dp) {
                    DownloadTab.values().forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageTab = DownloadTab.values()[page]
                    val pageDownloads by remember(uiState.downloads, pageTab, uiState.searchQuery) {
                        derivedStateOf { uiState.downloadsForTab(pageTab) }
                    }
                    if (pageDownloads.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (uiState.isSearchActive && uiState.searchQuery.isNotBlank())
                                    "No results for \"${uiState.searchQuery}\""
                                else "No downloads yet. Tap + to add one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(items = pageDownloads, key = { it.id }) { item ->
                                DownloadItemRow(
                                    item = item,
                                    onStart = { viewModel.onResume(item.id) },
                                    onPause = { viewModel.onPause(item.id) },
                                    onResume = { viewModel.onResume(item.id) },
                                    onCancel = { viewModel.onCancel(item.id) },
                                    onOpen = { viewModel.onOpenFile(item) },
                                    onShare = { viewModel.onShareFile(item) },
                                    onRedownload = { viewModel.showAddSheet(item.url) },
                                    onRedownloadWithOptions = { viewModel.onRedownloadWithOptions(item) },
                                    onRemove = { viewModel.onRemove(item.id) },
                                    onOpenFolder = { viewModel.onOpenFolder(item) },
                                    onCopyLink = { viewModel.onCopyLink(item) },
                                    onCopyMoveRename = { viewModel.onShowCopyMoveRename(item) },
                                    onProperties = { viewModel.onShowProperties(item) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Folder setup dialog
    if (uiState.showFolderSetupDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFolderSetupDialog() },
            title = { Text("Set Download Folder") },
            text = {
                Text(
                    "Downloads will be organized into:\n\n" +
                    "Download/QDM/Videos/\n" +
                    "Download/QDM/Music/\n" +
                    "Download/QDM/Documents/\n" +
                    "Download/QDM/Other/\n\n" +
                    "Use the default or choose a custom folder."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onFolderSetupUseDefault() }) { Text("Use Default") }
            },
            dismissButton = {
                TextButton(onClick = { folderPickerLauncher.launch(null) }) { Text("Choose Custom") }
            }
        )
    }

    // Add download sheet
    if (uiState.showAddSheet) {
        AddDownloadSheet(
            initialUrl = uiState.prefillUrl,
            onDismiss = { viewModel.dismissAddSheet() },
            onAdded = { viewModel.dismissAddSheet() },
            onStarted = { viewModel.dismissAddSheet() }
        )
    }

    // Properties dialog
    uiState.propertiesItem?.let { item ->
        DownloadPropertiesDialog(
            item = item,
            onDismiss = { viewModel.onDismissProperties() },
            onOpen = { viewModel.onOpenFile(item) },
            onShare = { viewModel.onShareFile(item) }
        )
    }

    // Copy/Move/Rename — simple placeholder dialog
    uiState.copyMoveRenameItem?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.onDismissCopyMoveRename() },
            title = { Text("Copy / Move / Rename") },
            text = { Text("File: ${item.fileName}\n\nThis feature is coming soon.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onDismissCopyMoveRename() }) { Text("OK") }
            }
        )
    }
}

/** Adds a list of URLs directly to the auto-queue (fire-and-forget). */
private fun QueueBulkImport(
    context: Context,
    urls: List<String>,
    viewModel: MainViewModel
) {
    // Minimal AddDownloadRequest for each URL — metadata will be fetched when it starts
    urls.forEach { url ->
        viewModel.queueBulkUrl(url)
    }
}
