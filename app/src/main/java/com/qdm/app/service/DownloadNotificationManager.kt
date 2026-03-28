package com.parveenbhadoo.qdm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.parveenbhadoo.qdm.MainActivity
import com.parveenbhadoo.qdm.R
import com.parveenbhadoo.qdm.domain.model.DownloadItem
import com.parveenbhadoo.qdm.utils.FormatUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_PROGRESS = "qdm_download_progress"
        const val CHANNEL_COMPLETE = "qdm_download_complete"
        const val CHANNEL_ERROR = "qdm_download_error"
        const val CHANNEL_SERVICE = "qdm_service"
        private const val SERVICE_NOTIFICATION_ID = 1
    }

    private val nm: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var lastNotificationTime = 0L

    private fun launchIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildServiceNotification(): Notification =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.notif_service_running))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launchIntent())
            .build()

    fun maybeNotifyProgress(item: DownloadItem) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < 1000L) return
        lastNotificationTime = now
        nm.notify(item.notificationId, buildProgressNotification(item))
    }

    fun notifyComplete(item: DownloadItem) {
        nm.cancel(item.notificationId)
        nm.notify(
            item.notificationId + 1_000_000,
            NotificationCompat.Builder(context, CHANNEL_COMPLETE)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.notif_complete))
                .setContentText(item.fileName)
                .setAutoCancel(true)
                .setContentIntent(launchIntent())
                .build()
        )
    }

    fun notifyError(item: DownloadItem) {
        nm.notify(
            item.notificationId,
            NotificationCompat.Builder(context, CHANNEL_ERROR)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.notif_error))
                .setContentText(item.fileName)
                .setSubText(item.errorMessage)
                .setAutoCancel(true)
                .setContentIntent(launchIntent())
                .build()
        )
    }

    fun cancelNotification(id: Int) = nm.cancel(id)

    private fun buildProgressNotification(item: DownloadItem): Notification {
        val progress = (item.progress * 100).toInt()
        val speed = FormatUtils.formatSpeed(item.speedBytesPerSec)
        val eta = FormatUtils.formatEta(item.etaSeconds)
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.fileName)
            .setContentText("$speed — $eta")
            .setProgress(100, progress, item.totalBytes <= 0)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(launchIntent())
            .build()
    }
}
