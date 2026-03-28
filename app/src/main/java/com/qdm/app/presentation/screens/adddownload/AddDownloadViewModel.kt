package com.qdm.app.presentation.screens.adddownload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdm.app.data.preferences.UserPreferencesDataStore
import com.qdm.app.domain.model.AddDownloadRequest
import com.qdm.app.domain.usecase.AddDownloadUseCase
import com.qdm.app.domain.usecase.FetchFileMetadataUseCase
import com.qdm.app.utils.FileUtils
import com.qdm.app.utils.QdmLog
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val prefs: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDownloadUiState())
    val uiState: StateFlow<AddDownloadUiState> = _uiState.asStateFlow()

    fun setInitialUrl(url: String) {
        _uiState.update { it.copy(url = url) }
    }

    fun onUrlChanged(url: String) = _uiState.update { it.copy(url = url, isFetched = false, error = null) }
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

    fun fetchInfo() {
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
                        threadCount = if (meta.supportsRanges) it.threadCount else 1
                    )
                }
            }.onFailure { e ->
                QdmLog.e("AddDownloadVM", "fetchInfo failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to fetch info") }
            }
        }
    }

    fun startDownload(onStarted: (String) -> Unit) {
        val state = _uiState.value
        if (state.fileName.isBlank() || state.url.isBlank()) return

        viewModelScope.launch {
            val settings = prefs.settingsFlow.first()
            val savePath = state.savePath.ifBlank { settings.defaultSavePath }
            val userAgent = state.userAgent.ifBlank { settings.defaultUserAgent }

            val request = AddDownloadRequest(
                url = state.url.trim(),
                fileName = FileUtils.sanitizeFileName(state.fileName),
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
                scheduledAt = state.scheduledAt
            )
            val id = addDownload.execute(request)
            QdmLog.i("AddDownloadVM", "Enqueued id=$id file=${request.fileName} savePath=${request.savePath.ifBlank { "<default>" }}")
            onStarted(id)
        }
    }
}
