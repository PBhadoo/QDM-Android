package com.parveenbhadoo.qdm.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parveenbhadoo.qdm.R
import com.parveenbhadoo.qdm.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.updateSavePath(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Downloads section
            SectionHeader(stringResource(R.string.section_downloads))

            val folderSubtitle = when {
                settings.defaultSavePath.isNotBlank() -> settings.defaultSavePath
                settings.folderSetupDone -> "Downloads/QDM/ (default)"
                else -> stringResource(R.string.not_set)
            }
            SettingItem(
                title = stringResource(R.string.default_save_folder),
                subtitle = folderSubtitle,
                onClick = { folderLauncher.launch(null) }
            )

            SliderSetting(
                title = "${stringResource(R.string.default_threads)}: ${settings.defaultThreadCount}",
                value = settings.defaultThreadCount.toFloat(),
                onValueChange = { viewModel.updateThreadCount(it.toInt()) },
                range = 1f..16f,
                steps = 14
            )

            SliderSetting(
                title = "${stringResource(R.string.max_concurrent)}: ${settings.maxConcurrentDownloads}",
                value = settings.maxConcurrentDownloads.toFloat(),
                onValueChange = { viewModel.updateMaxConcurrent(it.toInt()) },
                range = 1f..10f,
                steps = 8
            )

            SettingItem(
                title = stringResource(R.string.global_speed_limit),
                subtitle = if (settings.globalSpeedLimitBps == 0L)
                    stringResource(R.string.unlimited)
                else FormatUtils.formatSpeed(settings.globalSpeedLimitBps)
            )

            // Network section
            SectionHeader(stringResource(R.string.section_network))

            SwitchSetting(
                title = stringResource(R.string.wifi_only),
                subtitle = stringResource(R.string.wifi_only_desc),
                checked = settings.wifiOnlyMode,
                onCheckedChange = viewModel::updateWifiOnly
            )

            // Appearance section
            SectionHeader(stringResource(R.string.section_appearance))

            SwitchSetting(
                title = stringResource(R.string.dynamic_color),
                subtitle = stringResource(R.string.dynamic_color_desc),
                checked = settings.dynamicTheme,
                onCheckedChange = viewModel::updateDynamicTheme
            )

            val themeLabel = when (settings.darkMode) {
                true -> stringResource(R.string.theme_dark)
                false -> stringResource(R.string.theme_light)
                null -> stringResource(R.string.theme_system)
            }
            SettingItem(
                title = stringResource(R.string.theme),
                subtitle = themeLabel,
                onClick = {
                    val next = when (settings.darkMode) {
                        null -> "light"
                        false -> "dark"
                        true -> "system"
                    }
                    viewModel.updateDarkMode(next)
                }
            )

            // Notifications section
            SectionHeader(stringResource(R.string.section_notifications))

            SwitchSetting(
                title = stringResource(R.string.notification_progress),
                subtitle = stringResource(R.string.notification_progress_desc),
                checked = settings.notificationsEnabled,
                onCheckedChange = viewModel::updateNotifications
            )

            // About section
            SectionHeader(stringResource(R.string.section_about))

            SettingItem(
                title = stringResource(R.string.version),
                subtitle = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            )

            // Check for Updates
            val updateSubtitle = when (val s = updateState) {
                is UpdateCheckState.Idle -> "Tap to check"
                is UpdateCheckState.Checking -> stringResource(R.string.update_checking)
                is UpdateCheckState.UpToDate -> stringResource(R.string.update_up_to_date, s.version)
                is UpdateCheckState.UpdateAvailable -> stringResource(R.string.update_available, s.version)
                is UpdateCheckState.Failed -> stringResource(R.string.update_failed)
            }
            val updateClick: (() -> Unit) = when (val s = updateState) {
                is UpdateCheckState.UpdateAvailable -> {
                    { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.url))) }
                }
                is UpdateCheckState.Checking -> ({})
                else -> ({ viewModel.checkForUpdates() })
            }
            SettingItem(
                title = stringResource(R.string.check_for_updates),
                subtitle = updateSubtitle,
                subtitleColor = when (updateState) {
                    is UpdateCheckState.UpdateAvailable -> Color(0xFF4CAF50)
                    is UpdateCheckState.Failed -> MaterialTheme.colorScheme.error
                    else -> null
                },
                onClick = updateClick
            )

            // Made with love
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.made_with_love),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
