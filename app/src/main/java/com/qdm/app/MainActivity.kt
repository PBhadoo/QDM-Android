package com.parveenbhadoo.qdm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.parveenbhadoo.qdm.data.preferences.UserPreferencesDataStore
import com.parveenbhadoo.qdm.presentation.navigation.NavGraph
import com.parveenbhadoo.qdm.presentation.screens.main.MainViewModel
import com.parveenbhadoo.qdm.presentation.theme.QdmTheme
import com.parveenbhadoo.qdm.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataStore: UserPreferencesDataStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            val settings by preferencesDataStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.parveenbhadoo.qdm.domain.model.AppSettings()
            )
            val darkTheme = when (settings.darkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }
            QdmTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicTheme) {
                NavGraph()
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
                ?.let { NetworkUtils.extractUrl(it) }
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return

        // Post to main ViewModel via lifecycle scope is not directly possible here without
        // a shared ViewModel. Instead, we re-set the intent so NavGraph picks it up on the
        // next composition if needed — or use a SharedFlow.
        // For now, store in a companion object and let MainScreen consume it on resume.
        pendingUrl = url
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        val pendingUrlFlow = MutableStateFlow<String?>(null)
        var pendingUrl: String?
            get() = pendingUrlFlow.value
            set(value) { pendingUrlFlow.value = value }
    }
}
