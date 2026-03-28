package com.qdm.app.presentation.screens.browser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdm.app.data.repository.BrowserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserUiState(
    val currentUrl: String = "https://www.google.com",
    val pageTitle: String = "",
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val detectedMediaUrl: String? = null,
    val detectedMediaHeaders: Map<String, String> = emptyMap(),
    val adBlockedCount: Int = 0
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val browserRepository: BrowserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _adBlockHosts = MutableStateFlow<Set<String>>(emptySet())
    val adBlockHosts: StateFlow<Set<String>> = _adBlockHosts.asStateFlow()

    init {
        loadAdBlockHosts()
    }

    private fun loadAdBlockHosts() {
        viewModelScope.launch {
            try {
                val hosts = context.assets.open("adblock_hosts.txt").bufferedReader()
                    .useLines { lines ->
                        lines.map { it.trim().lowercase() }
                            .filter { it.isNotBlank() && !it.startsWith("#") }
                            .toHashSet()
                    }
                _adBlockHosts.value = hosts
            } catch (_: Exception) {
                _adBlockHosts.value = emptySet()
            }
        }
    }

    fun onPageStarted(url: String) = _uiState.update { it.copy(currentUrl = url, progress = 0) }

    fun onPageFinished(url: String, title: String) {
        _uiState.update { it.copy(currentUrl = url, pageTitle = title, progress = 100) }
        viewModelScope.launch { browserRepository.addHistory(url, title) }
    }

    fun onProgressChanged(progress: Int) = _uiState.update { it.copy(progress = progress) }

    fun onNavigationStateChanged(canBack: Boolean, canForward: Boolean) =
        _uiState.update { it.copy(canGoBack = canBack, canGoForward = canForward) }

    fun onMediaDetected(url: String, headers: Map<String, String>) =
        _uiState.update { it.copy(detectedMediaUrl = url, detectedMediaHeaders = headers) }

    fun dismissMedia() = _uiState.update { it.copy(detectedMediaUrl = null, detectedMediaHeaders = emptyMap()) }

    fun onAdBlocked() = _uiState.update { it.copy(adBlockedCount = it.adBlockedCount + 1) }

    fun setUrl(url: String) = _uiState.update { it.copy(currentUrl = url) }
}
