package com.qdm.app.presentation.screens.main

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdm.app.R
import com.qdm.app.presentation.components.DownloadItemRow
import com.qdm.app.presentation.components.MultiFab
import com.qdm.app.presentation.screens.adddownload.AddDownloadSheet
import com.qdm.app.utils.NetworkUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToBrowser: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onFolderSetupCustomPicked(it.toString())
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.downloads)) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.browser)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToBrowser("https://www.google.com")
                    }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            },
            floatingActionButton = {
                MultiFab(
                    onAddLink = { viewModel.showAddSheet() },
                    onPasteClipboard = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        val url = NetworkUtils.extractUrl(text) ?: ""
                        viewModel.showAddSheet(url)
                    },
                    onOpenBrowser = { onNavigateToBrowser("https://www.google.com") }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                ScrollableTabRow(selectedTabIndex = uiState.activeTab.ordinal, edgePadding = 0.dp) {
                    DownloadTab.values().forEachIndexed { index, tab ->
                        Tab(
                            selected = uiState.activeTab.ordinal == index,
                            onClick = { viewModel.selectTab(tab) },
                            text = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }

                if (uiState.filteredDownloads.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "No downloads yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(
                            items = uiState.filteredDownloads,
                            key = { it.id }
                        ) { item ->
                            DownloadItemRow(
                                item = item,
                                onPause = { viewModel.onPause(item.id) },
                                onResume = { viewModel.onResume(item.id) },
                                onCancel = { viewModel.onCancel(item.id) },
                                onOpen = { /* TODO: open file */ },
                                onShare = { /* TODO: share file */ },
                                onRedownload = { viewModel.showAddSheet(item.url) },
                                onRemove = { viewModel.onRemove(item.id) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

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
                TextButton(onClick = { viewModel.onFolderSetupUseDefault() }) {
                    Text("Use Default")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                    Text("Choose Custom")
                }
            }
        )
    }

    if (uiState.showAddSheet) {
        AddDownloadSheet(
            initialUrl = uiState.prefillUrl,
            onDismiss = { viewModel.dismissAddSheet() },
            onDownloadStarted = { downloadId ->
                viewModel.dismissAddSheet()
                viewModel.startDownload(downloadId)
            }
        )
    }
}
