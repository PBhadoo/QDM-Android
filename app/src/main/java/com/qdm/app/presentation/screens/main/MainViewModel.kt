package com.qdm.app.presentation.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdm.app.MainActivity
import com.qdm.app.data.preferences.UserPreferencesDataStore
import com.qdm.app.domain.engine.DownloadEngine
import com.qdm.app.domain.model.DownloadState
import com.qdm.app.domain.usecase.AddDownloadUseCase
import com.qdm.app.domain.usecase.CancelDownloadUseCase
import com.qdm.app.domain.usecase.GetDownloadsUseCase
import com.qdm.app.domain.usecase.PauseDownloadUseCase
import com.qdm.app.domain.usecase.ResumeDownloadUseCase
import com.qdm.app.utils.QdmLog
import dagger.hilt.android.lifecycle.HiltViewModel
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
        // Observe URLs coming from the browser or external intents
        viewModelScope.launch {
            MainActivity.pendingUrlFlow.filterNotNull().collect { url ->
                QdmLog.i("MainViewModel", "External URL received: $url")
                showAddSheet(url)
                MainActivity.pendingUrlFlow.value = null
            }
        }
        // Show folder setup dialog on first launch (when setup not yet acknowledged)
        viewModelScope.launch {
            prefs.isFolderSetupDoneFlow().first().let { done ->
                if (!done) {
                    QdmLog.i("MainViewModel", "Folder not configured — showing setup dialog")
                    _uiState.update { it.copy(showFolderSetupDialog = true) }
                }
            }
        }
    }

    /** User chose "Use Default" — no custom path needed, Downloads/QDM/{category} used automatically. */
    fun onFolderSetupUseDefault() {
        viewModelScope.launch {
            prefs.markFolderSetupDone()
            QdmLog.i("MainViewModel", "Folder setup: using default Downloads/QDM/")
            _uiState.update { it.copy(showFolderSetupDialog = false) }
        }
    }

    /** User chose a custom SAF folder. */
    fun onFolderSetupCustomPicked(uriString: String) {
        viewModelScope.launch {
            prefs.updateDefaultSavePath(uriString)
            prefs.markFolderSetupDone()
            QdmLog.i("MainViewModel", "Folder setup: custom path=$uriString")
            _uiState.update { it.copy(showFolderSetupDialog = false) }
        }
    }

    fun dismissFolderSetupDialog() {
        // Dismissed without picking — show again next time (setup not marked done)
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
            // Use .first() to read directly from DB — avoids race where uiState
            // hasn't yet been updated by the Room Flow after the insert.
            val item = getDownloads.execute().first().find { it.id == downloadId } ?: return@launch
            downloadEngine.startDownload(downloadId, item, viewModelScope)
        }
    }
}
