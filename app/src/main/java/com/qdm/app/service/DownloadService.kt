package com.parveenbhadoo.qdm.service

import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.lifecycleScope
import com.parveenbhadoo.qdm.data.repository.DownloadRepository
import com.parveenbhadoo.qdm.domain.engine.DownloadEngine
import com.parveenbhadoo.qdm.domain.model.DownloadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : androidx.lifecycle.LifecycleService() {

    companion object {
        const val ACTION_START = "com.parveenbhadoo.qdm.ACTION_START"
        const val ACTION_PAUSE = "com.parveenbhadoo.qdm.ACTION_PAUSE"
        const val ACTION_RESUME = "com.parveenbhadoo.qdm.ACTION_RESUME"
        const val ACTION_CANCEL = "com.parveenbhadoo.qdm.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "download_id"
        private const val NOTIFICATION_ID = 1
    }

    @Inject lateinit var downloadEngine: DownloadEngine
    @Inject lateinit var notificationManager: DownloadNotificationManager
    @Inject lateinit var repository: DownloadRepository

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, notificationManager.buildServiceNotification())
        acquireWakeLocks()
        observeDownloadStates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) {
            // Service restarted after kill — re-queue pending downloads
            lifecycleScope.launch {
                repository.getPendingDownloads().forEach { item ->
                    downloadEngine.startDownload(item.id, item, lifecycleScope)
                }
            }
            return START_STICKY
        }

        val id = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return START_STICKY
        when (intent.action) {
            ACTION_START, ACTION_RESUME -> lifecycleScope.launch {
                repository.getDownloadById(id)?.let { item ->
                    downloadEngine.startDownload(id, item, lifecycleScope)
                }
            }
            ACTION_PAUSE -> lifecycleScope.launch {
                downloadEngine.pauseDownload(id)
                repository.updateState(id, DownloadState.Paused)
            }
            ACTION_CANCEL -> lifecycleScope.launch {
                downloadEngine.cancelDownload(id)
                repository.updateState(id, DownloadState.Cancelled)
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Service continues even when app is swiped — START_STICKY handles restart
    }

    override fun onDestroy() {
        releaseWakeLocks()
        super.onDestroy()
    }

    private fun observeDownloadStates() {
        lifecycleScope.launch {
            downloadEngine.stateFlow.collectLatest { stateMap ->
                stateMap.forEach { (id, state) ->
                    when (state) {
                        is DownloadState.Downloading -> {
                            repository.getDownloadById(id)?.let { item ->
                                val updated = item.copy(
                                    speedBytesPerSec = state.speedBps,
                                    etaSeconds = state.etaSeconds,
                                    downloadedBytes = state.downloadedBytes
                                )
                                notificationManager.maybeNotifyProgress(updated)
                            }
                        }
                        is DownloadState.Completed -> {
                            repository.getDownloadById(id)?.let {
                                notificationManager.notifyComplete(it)
                            }
                            stopIfQueueEmpty()
                        }
                        is DownloadState.Error -> {
                            repository.getDownloadById(id)?.let {
                                notificationManager.notifyError(it.copy(errorMessage = state.message))
                            }
                            stopIfQueueEmpty()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private suspend fun stopIfQueueEmpty() {
        if (!downloadEngine.hasActiveDownloads() && repository.getNextQueuedDownloads(1).isEmpty()) {
            releaseWakeLocks()
            stopSelf()
        }
    }

    private fun acquireWakeLocks() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "QDM::DownloadWakeLock"
        ).also { it.acquire(10 * 60 * 60 * 1000L) } // 10 hours max

        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiLockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        else
            @Suppress("DEPRECATION") WifiManager.WIFI_MODE_FULL_HIGH_PERF
        wifiLock = wm.createWifiLock(wifiLockMode, "QDM::DownloadWifiLock").also { it.acquire() }
    }

    private fun releaseWakeLocks() {
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        try { wifiLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
    }
}
