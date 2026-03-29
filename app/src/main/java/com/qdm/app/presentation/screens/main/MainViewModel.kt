package com.parveenbhadoo.qdm.presentation.screens.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parveenbhadoo.qdm.MainActivity
import com.parveenbhadoo.qdm.data.preferences.UserPreferencesDataStore
import com.parveenbhadoo.qdm.domain.engine.DownloadEngine
import com.parveenbhadoo.qdm.domain.model.DownloadItem
import com.parveenbhadoo.qdm.domain.model.DownloadState
import com.parveenbhadoo.qdm.domain.model.AddDownloadRequest
import com.parveenbhadoo.qdm.domain.usecase.AddDownloadUseCase
import com.parveenbhadoo.qdm.utils.FileUtils
import com.parveenbhadoo.qdm.domain.usecase.CancelDownloadUseCase
import com.parveenbhadoo.qdm.domain.usecase.GetDownloadsUseCase
import com.parveenbhadoo.qdm.domain.usecase.PauseDownloadUseCase
import com.parveenbhadoo.qdm.domain.usecase.ResumeDownloadUseCase
import com.parveenbhadoo.qdm.utils.QdmLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getDownloads: GetDownloadsUseCase,
    private val pauseDownload: PauseDownloadUseCase,
    private val resumeDownload: ResumeDownloadUseCase,
    private val cancelDownload: CancelDownloadUseCase,
    private val addDownload: AddDownloadUseCase,
    private val downloadEngine: DownloadEngine,
    private val prefs: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getDownloads.execute(),
                downloadEngine.stateFlow
            ) { entities, liveStates ->
                entities.map { item ->
                    val liveState = liveStates[item.id]
                    if (liveState != null) item.copy(state = liveState) else item
                }
            }.collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }
        viewModelScope.launch {
            MainActivity.pendingUrlFlow.filterNotNull().collect { url ->
                QdmLog.i("MainViewModel", "External URL received: $url")
                showAddSheet(url)
                MainActivity.pendingUrlFlow.value = null
            }
        }
        viewModelScope.launch {
            prefs.isFolderSetupDoneFlow().first().let { done ->
                if (!done) {
                    QdmLog.i("MainViewModel", "Folder not configured — showing setup dialog")
                    _uiState.update { it.copy(showFolderSetupDialog = true) }
                }
            }
        }
    }

    fun onFolderSetupUseDefault() {
        viewModelScope.launch {
            prefs.markFolderSetupDone()
            _uiState.update { it.copy(showFolderSetupDialog = false) }
        }
    }

    fun onFolderSetupCustomPicked(uriString: String) {
        viewModelScope.launch {
            prefs.updateDefaultSavePath(uriString)
            prefs.markFolderSetupDone()
            _uiState.update { it.copy(showFolderSetupDialog = false) }
        }
    }

    fun dismissFolderSetupDialog() {
        _uiState.update { it.copy(showFolderSetupDialog = false) }
    }

    fun selectTab(tab: DownloadTab) = _uiState.update { it.copy(activeTab = tab) }

    fun onSearchQueryChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun toggleSearch() = _uiState.update { it.copy(isSearchActive = !it.isSearchActive, searchQuery = "") }

    fun showAddSheet(url: String = "") = _uiState.update { it.copy(showAddSheet = true, prefillUrl = url) }

    fun dismissAddSheet() = _uiState.update { it.copy(showAddSheet = false, prefillUrl = "") }

    fun onExternalUrl(url: String) = showAddSheet(url)

    fun onPause(id: String) = viewModelScope.launch { pauseDownload.execute(id) }

    fun onResume(id: String) = viewModelScope.launch { resumeDownload.execute(id) }

    fun onCancel(id: String) = viewModelScope.launch { cancelDownload.execute(id) }

    fun onRemove(id: String) = viewModelScope.launch { cancelDownload.execute(id, deleteFile = true) }

    fun startDownload(downloadId: String) {
        viewModelScope.launch {
            val item = getDownloads.execute().first().find { it.id == downloadId } ?: return@launch
            downloadEngine.startDownload(downloadId, item, viewModelScope)
        }
    }

    // --- Per-item actions ---

    fun onOpenFile(item: DownloadItem) {
        if (item.savePath.isBlank()) return
        try {
            val uri = item.savePath.toUri()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            QdmLog.w("MainViewModel", "Cannot open file: ${e.message}")
        }
    }

    fun onShareFile(item: DownloadItem) {
        if (item.savePath.isBlank()) return
        try {
            val uri = item.savePath.toUri()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, item.fileName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            QdmLog.w("MainViewModel", "Cannot share file: ${e.message}")
        }
    }

    fun onOpenFolder(item: DownloadItem) {
        try {
            // Try to open in system Files app — navigate to the Downloads folder
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FQDM"), "vnd.android.document/directory")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open generic file manager
            try {
                val fallback = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_FILES)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e2: Exception) {
                QdmLog.w("MainViewModel", "Cannot open folder: ${e2.message}")
            }
        }
    }

    fun onCopyLink(item: DownloadItem) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("download url", item.url))
    }

    fun onRedownloadWithOptions(item: DownloadItem) {
        showAddSheet(item.url)
    }

    fun onShowProperties(item: DownloadItem) {
        _uiState.update { it.copy(propertiesItem = item) }
    }

    fun onDismissProperties() {
        _uiState.update { it.copy(propertiesItem = null) }
    }

    fun onShowCopyMoveRename(item: DownloadItem) {
        _uiState.update { it.copy(copyMoveRenameItem = item) }
    }

    fun onDismissCopyMoveRename() {
        _uiState.update { it.copy(copyMoveRenameItem = null) }
    }

    /** Adds a URL directly to the auto-queue without showing the AddDownloadSheet. */
    fun queueBulkUrl(url: String) {
        viewModelScope.launch {
            val fileName = FileUtils.extractFileNameFromUrl(url)
            val settings = prefs.settingsFlow.first()
            val request = AddDownloadRequest(
                url = url,
                fileName = FileUtils.sanitizeFileName(fileName),
                savePath = settings.defaultSavePath,
                threadCount = settings.defaultThreadCount,
                userAgent = settings.defaultUserAgent
            )
            addDownload.execute(request, isQueued = true)
            QdmLog.i("MainViewModel", "Bulk-queued: $url")
        }
    }
}
