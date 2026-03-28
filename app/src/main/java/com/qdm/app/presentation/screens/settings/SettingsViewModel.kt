package com.parveenbhadoo.qdm.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parveenbhadoo.qdm.data.repository.SettingsRepository
import com.parveenbhadoo.qdm.domain.model.AppSettings
import com.parveenbhadoo.qdm.utils.QdmLog
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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    data class UpToDate(val version: String) : UpdateCheckState()
    data class UpdateAvailable(val version: String, val apkUrl: String) : UpdateCheckState()
    data class Downloading(val progress: Int) : UpdateCheckState()
    object ReadyToInstall : UpdateCheckState()
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
                val apkUrl = json.substringAfter("\"assets\":[", "")
                    .split("\"browser_download_url\":")
                    .drop(1)
                    .map { it.substringAfter("\"").substringBefore("\"") }
                    .firstOrNull { it.endsWith(".apk") } ?: ""
                val currentVersion = "v" + context.packageManager
                    .getPackageInfo(context.packageName, 0).versionName
                _updateState.value = when {
                    tagName.isBlank() -> UpdateCheckState.Failed
                    tagName == currentVersion -> UpdateCheckState.UpToDate(currentVersion)
                    apkUrl.isBlank() -> UpdateCheckState.Failed
                    else -> UpdateCheckState.UpdateAvailable(tagName, apkUrl)
                }
            }.onFailure { e ->
                QdmLog.e("SettingsVM", "Update check failed: ${e.message}")
                _updateState.value = UpdateCheckState.Failed
            }
        }
    }

    fun downloadAndInstall(apkUrl: String) {
        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Downloading(0)
            runCatching {
                withContext(Dispatchers.IO) {
                    val cacheFile = File(context.cacheDir, "qdm-update.apk")
                    val conn = URL(apkUrl).openConnection() as HttpURLConnection
                    conn.connect()
                    val totalBytes = conn.contentLengthLong
                    var downloaded = 0L
                    conn.inputStream.use { input ->
                        cacheFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 0) {
                                    val pct = (downloaded * 100 / totalBytes).toInt()
                                    _updateState.value = UpdateCheckState.Downloading(pct)
                                }
                            }
                        }
                    }
                    conn.disconnect()
                    cacheFile
                }
            }.onSuccess { file ->
                _updateState.value = UpdateCheckState.ReadyToInstall
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = uri
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }.onFailure { e ->
                QdmLog.e("SettingsVM", "Update download failed: ${e.message}")
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
