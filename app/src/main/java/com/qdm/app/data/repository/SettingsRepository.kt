package com.qdm.app.data.repository

import com.qdm.app.data.preferences.UserPreferencesDataStore
import com.qdm.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    suspend fun updateDefaultSavePath(uri: String) = dataStore.updateDefaultSavePath(uri)
    suspend fun updateThreadCount(count: Int) = dataStore.updateThreadCount(count)
    suspend fun updateMaxConcurrent(count: Int) = dataStore.updateMaxConcurrent(count)
    suspend fun updateSpeedLimit(bps: Long) = dataStore.updateSpeedLimit(bps)
    suspend fun updateWifiOnly(enabled: Boolean) = dataStore.updateWifiOnly(enabled)
    suspend fun updateNotifications(enabled: Boolean) = dataStore.updateNotifications(enabled)
    suspend fun updateDynamicTheme(enabled: Boolean) = dataStore.updateDynamicTheme(enabled)
    suspend fun updateDarkMode(mode: String) = dataStore.updateDarkMode(mode)
    suspend fun updateUserAgent(ua: String) = dataStore.updateUserAgent(ua)
    suspend fun updateLanguage(tag: String) = dataStore.updateLanguage(tag)
}
