package com.parveenbhadoo.qdm.presentation.screens.adddownload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parveenbhadoo.qdm.data.preferences.UserPreferencesDataStore
import com.parveenbhadoo.qdm.domain.model.AddDownloadRequest
import com.parveenbhadoo.qdm.domain.usecase.AddDownloadUseCase
import com.parveenbhadoo.qdm.domain.usecase.FetchFileMetadataUseCase
import com.parveenbhadoo.qdm.domain.usecase.ResumeDownloadUseCase
import com.parveenbhadoo.qdm.utils.FileUtils
import com.parveenbhadoo.qdm.utils.QdmLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDownloadViewModel @Inject constructor(
    private val fetchMetadata: FetchFileMetadataUseCase,
    private val addDownload: AddDownloadUseCase,
    private val resumeDownload: ResumeDownloadUseCase,
    private val prefs: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDownloadUiState())
    val uiState: StateFlow<AddDownloadUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        // Load defaults so the sheet shows them immediately
        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            _uiState.update { it.copy(savePath = settings.defaultSavePath, userAgent = settings.defaultUserAgent) }
        }
    }

    fun setInitialUrl(url: String, referer: String = "", cookies: String = "", userAgent: String = "") {
        _uiState.update { state ->
            state.copy(
                url = url,
                referer = referer,
                cookies = cookies,
                userAgent = userAgent.ifBlank { state.userAgent }
            )
        }
        scheduleAutoFetch(url)
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(url = url, isFetched = false, error = null, fileName = "", totalBytes = -1L) }
        scheduleAutoFetch(url)
    }

    /** Debounced auto-fetch: fires 800ms after the user stops typing. */
    private fun scheduleAutoFetch(url: String) {
        fetchJob?.cancel()
        val trimmed = url.trim()
        if (trimmed.isBlank() || (!trimmed.startsWith("http://") && !trimmed.startsWith("https://"))) return
        fetchJob = viewModelScope.launch {
            delay(800)
            fetchInfoSilent()
        }
    }

    /** Fetch metadata; on failure just log — never show error in UI. */
    private fun fetchInfoSilent() {
        val url = _uiState.value.url.trim()
        if (url.isBlank()) return
        QdmLog.d("AddDownloadVM", "fetchInfo url=$url")
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val headers = mutableMapOf<String, String>()
            _uiState.value.referer.takeIf { it.isNotBlank() }?.let { headers["Referer"] = it }
            _uiState.value.cookies.takeIf { it.isNotBlank() }?.let { headers["Cookie"] = it }

            fetchMetadata.execute(url, headers).onSuccess { meta ->
                QdmLog.i("AddDownloadVM", "Meta: file=${meta.fileName} size=${meta.totalBytes} mime=${meta.mimeType} ranges=${meta.supportsRanges}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isFetched = true,
                        fileName = if (it.fileName.isBlank()) meta.fileName else it.fileName,
                        totalBytes = meta.totalBytes,
                        mimeType = meta.mimeType,
                        supportsRanges = meta.supportsRanges,
                        threadCount = if (meta.supportsRanges) it.threadCount else 1
                    )
                }
            }.onFailure { e ->
                QdmLog.w("AddDownloadVM", "fetchInfo failed: ${e.message}")
                val fallbackName = FileUtils.extractFileNameFromUrl(url)
                _uiState.update { it.copy(
                    isLoading = false,
                    isFetched = true,
                    fileName = if (it.fileName.isBlank()) fallbackName else it.fileName
                ) }
            }
        }
    }

    fun onRefererChanged(v: String) = _uiState.update { it.copy(referer = v) }
    fun onFileNameChanged(v: String) = _uiState.update { it.copy(fileName = v) }
    fun onThreadCountChanged(v: Int) = _uiState.update { it.copy(threadCount = v.coerceIn(1, 16)) }
    fun onUserAgentChanged(v: String) = _uiState.update { it.copy(userAgent = v) }
    fun onSpeedLimitChanged(v: Long) = _uiState.update { it.copy(speedLimitBps = v) }
    fun onUsernameChanged(v: String) = _uiState.update { it.copy(username = v) }
    fun onPasswordChanged(v: String) = _uiState.update { it.copy(password = v) }
    fun onCookiesChanged(v: String) = _uiState.update { it.copy(cookies = v) }
    fun toggleAdvanced() = _uiState.update { it.copy(showAdvanced = !it.showAdvanced) }
    fun onSavePathSelected(uri: String) = _uiState.update { it.copy(savePath = uri) }

    /**
     * "Add" — adds to Pending (manual). User must tap Start to begin.
     * Derives filename from URL if metadata not yet fetched.
     */
    fun addToQueue(onAdded: (String) -> Unit) {
        enqueue(autoStart = false, isQueued = false, onDone = onAdded)
    }

    /**
     * "Start" — enqueue and immediately begin downloading.
     * Only enabled once filename + size are known.
     */
    fun addAndStart(onStarted: (String) -> Unit) {
        enqueue(autoStart = true, isQueued = false, onDone = onStarted)
    }

    /**
     * Bulk import helper — adds to Queue (auto-starts when a slot opens).
     */
    fun addToAutoQueue(onAdded: (String) -> Unit) {
        enqueue(autoStart = false, isQueued = true, onDone = onAdded)
    }

    private fun enqueue(autoStart: Boolean, isQueued: Boolean = false, onDone: (String) -> Unit) {
        val state = _uiState.value
        if (state.url.isBlank()) return

        val fileName = state.fileName.ifBlank {
            FileUtils.extractFileNameFromUrl(state.url)
        }

        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val savePath = state.savePath.ifBlank { settings.defaultSavePath }
            val userAgent = state.userAgent.ifBlank { settings.defaultUserAgent }

            val request = AddDownloadRequest(
                url = state.url.trim(),
                fileName = FileUtils.sanitizeFileName(fileName),
                savePath = savePath,
                mimeType = state.mimeType,
                totalBytes = state.totalBytes,
                referer = state.referer.takeIf { it.isNotBlank() },
                userAgent = userAgent,
                cookies = state.cookies.takeIf { it.isNotBlank() },
                threadCount = state.threadCount,
                speedLimitBps = state.speedLimitBps,
                username = state.username.takeIf { it.isNotBlank() },
                password = state.password.takeIf { it.isNotBlank() },
                scheduledAt = state.scheduledAt,
                supportsRanges = state.supportsRanges
            )
            val id = addDownload.execute(request, isQueued = isQueued)
            QdmLog.i("AddDownloadVM", "${if (autoStart) "AddAndStart" else if (isQueued) "AddQueued" else "AddPending"} id=$id file=${request.fileName}")
            if (autoStart) resumeDownload.execute(id)
            onDone(id)
        }
    }
}
