package com.qdm.app.presentation.screens.settings

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdm.app.data.repository.SettingsRepository
import com.qdm.app.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

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
