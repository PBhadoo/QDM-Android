package com.qdm.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qdm.app.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qdm_settings")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SAVE_PATH = stringPreferencesKey("save_path")
        val FOLDER_SETUP_DONE = booleanPreferencesKey("folder_setup_done")
        val THREAD_COUNT = intPreferencesKey("thread_count")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val SPEED_LIMIT = longPreferencesKey("speed_limit")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
        val DARK_MODE = stringPreferencesKey("dark_mode") // "system", "light", "dark"
        val USER_AGENT = stringPreferencesKey("user_agent")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { prefs ->
            val darkModeStr = prefs[Keys.DARK_MODE] ?: "system"
            AppSettings(
                defaultSavePath = prefs[Keys.SAVE_PATH] ?: "",
                defaultThreadCount = prefs[Keys.THREAD_COUNT] ?: 4,
                maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT] ?: 3,
                globalSpeedLimitBps = prefs[Keys.SPEED_LIMIT] ?: 0L,
                wifiOnlyMode = prefs[Keys.WIFI_ONLY] ?: false,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
                dynamicTheme = prefs[Keys.DYNAMIC_THEME] ?: true,
                darkMode = when (darkModeStr) {
                    "light" -> false
                    "dark" -> true
                    else -> null
                },
                defaultUserAgent = prefs[Keys.USER_AGENT] ?: AppSettings.DEFAULT_USER_AGENT,
                appLanguage = prefs[Keys.LANGUAGE] ?: "system"
            )
        }

    suspend fun updateDefaultSavePath(uri: String) =
        context.dataStore.edit { it[Keys.SAVE_PATH] = uri }

    suspend fun updateThreadCount(count: Int) =
        context.dataStore.edit { it[Keys.THREAD_COUNT] = count }

    suspend fun updateMaxConcurrent(count: Int) =
        context.dataStore.edit { it[Keys.MAX_CONCURRENT] = count }

    suspend fun updateSpeedLimit(bps: Long) =
        context.dataStore.edit { it[Keys.SPEED_LIMIT] = bps }

    suspend fun updateWifiOnly(enabled: Boolean) =
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }

    suspend fun updateNotifications(enabled: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }

    suspend fun updateDynamicTheme(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DYNAMIC_THEME] = enabled }

    suspend fun updateDarkMode(mode: String) =
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }

    suspend fun updateUserAgent(ua: String) =
        context.dataStore.edit { it[Keys.USER_AGENT] = ua }

    suspend fun updateLanguage(tag: String) =
        context.dataStore.edit { it[Keys.LANGUAGE] = tag }

    fun isFolderSetupDoneFlow(): kotlinx.coroutines.flow.Flow<Boolean> =
        context.dataStore.data
            .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
            .map { it[Keys.FOLDER_SETUP_DONE] ?: false }

    suspend fun markFolderSetupDone() =
        context.dataStore.edit { it[Keys.FOLDER_SETUP_DONE] = true }
}
