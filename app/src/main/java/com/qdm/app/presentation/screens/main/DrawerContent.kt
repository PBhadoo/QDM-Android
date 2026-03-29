package com.parveenbhadoo.qdm.presentation.screens.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parveenbhadoo.qdm.R
import com.parveenbhadoo.qdm.presentation.screens.settings.SettingsViewModel
import com.parveenbhadoo.qdm.presentation.screens.settings.UpdateCheckState
import com.parveenbhadoo.qdm.utils.FormatUtils

/**
 * Full-featured settings page rendered inside the navigation drawer.
 * Mirrors SettingsScreen but lives inline in the drawer — no navigation needed.
 */
@Composable
fun DrawerContent(
    onClose: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val updateState by settingsViewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSpeedDialog by remember { mutableStateOf(false) }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settingsViewModel.updateSavePath(it.toString())
        }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QDM",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close menu")
                }
            }
            HorizontalDivider()
        }

        // Downloads section
        item { DrawerSectionHeader(stringResource(R.string.section_downloads)) }
        item {
            val folderSubtitle = when {
                settings.defaultSavePath.isNotBlank() -> settings.defaultSavePath
                settings.folderSetupDone -> "Downloads/QDM/ (default)"
                else -> stringResource(R.string.not_set)
            }
            DrawerSettingItem(
                title = stringResource(R.string.default_save_folder),
                subtitle = folderSubtitle,
                onClick = { folderLauncher.launch(null) }
            )
        }
        item {
            DrawerSliderSetting(
                title = "${stringResource(R.string.default_threads)}: ${settings.defaultThreadCount}",
                value = settings.defaultThreadCount.toFloat(),
                onValueChange = { settingsViewModel.updateThreadCount(it.toInt()) },
                range = 1f..16f,
                steps = 14
            )
        }
        item {
            DrawerSliderSetting(
                title = "${stringResource(R.string.max_concurrent)}: ${settings.maxConcurrentDownloads}",
                value = settings.maxConcurrentDownloads.toFloat(),
                onValueChange = { settingsViewModel.updateMaxConcurrent(it.toInt()) },
                range = 1f..10f,
                steps = 8
            )
        }
        item {
            DrawerSettingItem(
                title = stringResource(R.string.global_speed_limit),
                subtitle = if (settings.globalSpeedLimitBps == 0L) stringResource(R.string.unlimited)
                else FormatUtils.formatSpeed(settings.globalSpeedLimitBps),
                onClick = { showSpeedDialog = true }
            )
        }

        // Network section
        item { DrawerSectionHeader(stringResource(R.string.section_network)) }
        item {
            DrawerSwitchSetting(
                title = stringResource(R.string.wifi_only),
                subtitle = stringResource(R.string.wifi_only_desc),
                checked = settings.wifiOnlyMode,
                onCheckedChange = settingsViewModel::updateWifiOnly
            )
        }

        // Device & Storage section
        item { DrawerSectionHeader("Device & Storage") }
        item {
            val stat = remember { StatFs(Environment.getDataDirectory().path) }
            val total = stat.totalBytes
            val free = stat.freeBytes
            val used = total - free
            DrawerSettingItem(
                title = "Internal Storage",
                subtitle = "${FormatUtils.formatBytes(used)} used of ${FormatUtils.formatBytes(total)} · ${FormatUtils.formatBytes(free)} free"
            )
        }
        item {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = remember { ActivityManager.MemoryInfo().also { actManager.getMemoryInfo(it) } }
            DrawerSettingItem(
                title = "RAM",
                subtitle = "${FormatUtils.formatBytes(memInfo.totalMem)} total · ${FormatUtils.formatBytes(memInfo.availMem)} available"
            )
        }
        item {
            DrawerSettingItem(
                title = "Device",
                subtitle = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            )
        }
        item {
            DrawerSettingItem(
                title = "Android",
                subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
        }

        // Appearance section
        item { DrawerSectionHeader(stringResource(R.string.section_appearance)) }
        item {
            DrawerSwitchSetting(
                title = stringResource(R.string.dynamic_color),
                subtitle = stringResource(R.string.dynamic_color_desc),
                checked = settings.dynamicTheme,
                onCheckedChange = settingsViewModel::updateDynamicTheme
            )
        }
        item {
            val themeLabel = when (settings.darkMode) {
                true -> stringResource(R.string.theme_dark)
                false -> stringResource(R.string.theme_light)
                null -> stringResource(R.string.theme_system)
            }
            DrawerSettingItem(
                title = stringResource(R.string.theme),
                subtitle = themeLabel,
                onClick = {
                    val next = when (settings.darkMode) {
                        null -> "light"
                        false -> "dark"
                        true -> "system"
                    }
                    settingsViewModel.updateDarkMode(next)
                }
            )
        }

        // Notifications section
        item { DrawerSectionHeader(stringResource(R.string.section_notifications)) }
        item {
            DrawerSwitchSetting(
                title = stringResource(R.string.notification_progress),
                subtitle = stringResource(R.string.notification_progress_desc),
                checked = settings.notificationsEnabled,
                onCheckedChange = settingsViewModel::updateNotifications
            )
        }

        // About section
        item { DrawerSectionHeader(stringResource(R.string.section_about)) }
        item {
            DrawerSettingItem(
                title = stringResource(R.string.version),
                subtitle = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            )
        }
        item {
            val updateSubtitle = when (val s = updateState) {
                is UpdateCheckState.Idle -> "Tap to check for updates"
                is UpdateCheckState.Checking -> stringResource(R.string.update_checking)
                is UpdateCheckState.UpToDate -> stringResource(R.string.update_up_to_date, s.version)
                is UpdateCheckState.UpdateAvailable -> stringResource(R.string.update_available, s.version)
                is UpdateCheckState.Downloading -> "Downloading… ${s.progress}%"
                is UpdateCheckState.ReadyToInstall -> "Installing…"
                is UpdateCheckState.Failed -> stringResource(R.string.update_failed)
            }
            val updateClick: () -> Unit = when (val s = updateState) {
                is UpdateCheckState.UpdateAvailable -> ({ settingsViewModel.downloadAndInstall(s.apkUrl) })
                is UpdateCheckState.Checking,
                is UpdateCheckState.Downloading,
                is UpdateCheckState.ReadyToInstall -> ({})
                else -> ({ settingsViewModel.checkForUpdates() })
            }
            DrawerSettingItem(
                title = stringResource(R.string.check_for_updates),
                subtitle = updateSubtitle,
                subtitleColor = when (updateState) {
                    is UpdateCheckState.UpdateAvailable -> Color(0xFF4CAF50)
                    is UpdateCheckState.Downloading -> MaterialTheme.colorScheme.primary
                    is UpdateCheckState.Failed -> MaterialTheme.colorScheme.error
                    else -> null
                },
                onClick = updateClick
            )
        }

        item {
            DrawerSettingItem(
                title = "Report an Issue",
                subtitle = "Open GitHub Issues page",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/PBhadoo/QDM-Android/issues"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }
        item {
            DrawerSettingItem(
                title = "Telegram Channel",
                subtitle = "t.me/HashHackers",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/HashHackers"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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

    if (showSpeedDialog) {
        SpeedLimitDialog(
            currentBps = settings.globalSpeedLimitBps,
            onDismiss = { showSpeedDialog = false },
            onConfirm = { bps ->
                settingsViewModel.updateSpeedLimit(bps)
                showSpeedDialog = false
            }
        )
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerSettingItem(
    title: String,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
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
private fun DrawerSwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
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
private fun DrawerSliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(2.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

private enum class SpeedUnit { UNLIMITED, KB, MB }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedLimitDialog(
    currentBps: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val (initUnit, initValue) = when {
        currentBps == 0L -> SpeedUnit.UNLIMITED to ""
        currentBps < 1024L * 1024L -> SpeedUnit.KB to (currentBps / 1024L).toString()
        else -> SpeedUnit.MB to (currentBps / 1024L / 1024L).toString()
    }

    var unit by remember { mutableStateOf(initUnit) }
    var inputValue by remember { mutableStateOf(initValue) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speed Limit") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = if (unit == SpeedUnit.UNLIMITED) "∞" else inputValue,
                    onValueChange = { newVal ->
                        if (unit == SpeedUnit.UNLIMITED) return@OutlinedTextField
                        val digits = newVal.filter { it.isDigit() }.take(4)
                        val num = digits.toIntOrNull() ?: 0
                        if (num > 1023 && unit == SpeedUnit.KB) {
                            inputValue = "1"
                            unit = SpeedUnit.MB
                        } else {
                            inputValue = digits
                        }
                    },
                    enabled = unit != SpeedUnit.UNLIMITED,
                    singleLine = true,
                    label = { Text("Value") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    TextButton(onClick = { unitMenuExpanded = true }) {
                        Text(
                            when (unit) {
                                SpeedUnit.KB -> "KB/s"
                                SpeedUnit.MB -> "MB/s"
                                SpeedUnit.UNLIMITED -> "∞"
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unlimited") },
                            onClick = { unit = SpeedUnit.UNLIMITED; unitMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("KB/s") },
                            onClick = {
                                unit = SpeedUnit.KB
                                if (inputValue.isBlank()) inputValue = "512"
                                unitMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("MB/s") },
                            onClick = {
                                unit = SpeedUnit.MB
                                if (inputValue.isBlank()) inputValue = "1"
                                unitMenuExpanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val bps = when (unit) {
                    SpeedUnit.UNLIMITED -> 0L
                    SpeedUnit.KB -> (inputValue.toLongOrNull() ?: 0L) * 1024L
                    SpeedUnit.MB -> (inputValue.toLongOrNull() ?: 0L) * 1024L * 1024L
                }
                onConfirm(bps)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
