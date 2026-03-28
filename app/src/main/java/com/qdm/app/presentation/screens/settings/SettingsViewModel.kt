package com.parveenbhadoo.qdm.presentation.screens.settings

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parveenbhadoo.qdm.data.repository.SettingsRepository
import com.parveenbhadoo.qdm.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpToDate(val version: String) : UpdateCheckState()
    data class UpdateAvailable(val version: String, val url: String) : UpdateCheckState()
    object Failed : UpdateCheckState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Checking
            runCatching {
                withContext(Dispatchers.IO) {
                    val conn = URL("https://api.github.com/repos/PBhadoo/QDM-Android/releases/latest")
                        .openConnection() as HttpURLConnection
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 10_000
                    conn.connect()
                    conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
                }
            }.onSuccess { json ->
                val tagName = json.substringAfter("\"tag_name\":\"", "").substringBefore("\"").trim()
                val htmlUrl = json.substringAfter("\"html_url\":\"", "").substringBefore("\"").trim()
                val currentVersion = "v" + context.packageManager
                    .getPackageInfo(context.packageName, 0).versionName
                _updateState.value = when {
                    tagName.isBlank() -> UpdateCheckState.Failed
                    tagName == currentVersion -> UpdateCheckState.UpToDate(currentVersion)
                    else -> UpdateCheckState.UpdateAvailable(tagName, htmlUrl)
                }
            }.onFailure {
                _updateState.value = UpdateCheckState.Failed
            }
        }
    }

    fun updateSavePath(uri: String) = viewModelScope.launch {
        settingsRepository.updateDefaultSavePath(uri)
    }

    fun updateThreadCount(count: Int) = viewModelScope.launch {
        settingsRepository.updateThreadCount(count)
    }

    fun updateMaxConcurrent(count: Int) = viewModelScope.launch {
        settingsRepository.updateMaxConcurrent(count)
    }

    fun updateSpeedLimit(bps: Long) = viewModelScope.launch {
        settingsRepository.updateSpeedLimit(bps)
    }

    fun updateWifiOnly(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateWifiOnly(enabled)
    }

    fun updateNotifications(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateNotifications(enabled)
    }

    fun updateDynamicTheme(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updateDynamicTheme(enabled)
    }

    fun updateDarkMode(mode: String) = viewModelScope.launch {
        settingsRepository.updateDarkMode(mode)
    }

    fun updateUserAgent(ua: String) = viewModelScope.launch {
        settingsRepository.updateUserAgent(ua)
    }

    fun updateLanguage(languageTag: String) = viewModelScope.launch {
        settingsRepository.updateLanguage(languageTag)
        applyLanguage(languageTag)
    }

    private fun applyLanguage(tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(android.app.LocaleManager::class.java)
                .applicationLocales = android.os.LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(
                if (tag == "system") LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(tag)
            )
        }
    }
}
