package com.qdm.app.presentation.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdm.app.R
import com.qdm.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
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

            SettingItem(
                title = stringResource(R.string.default_save_folder),
                subtitle = settings.defaultSavePath.ifBlank { stringResource(R.string.not_set) },
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
                subtitle = "1.0.0"
            )
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
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null)
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    else
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)

    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
