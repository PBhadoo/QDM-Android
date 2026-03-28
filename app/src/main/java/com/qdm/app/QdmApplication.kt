package com.qdm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.qdm.app.service.DownloadNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QdmApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        DownloadNotificationManager.CHANNEL_PROGRESS,
                        "Download Progress",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "Shows active download progress" },

                    NotificationChannel(
                        DownloadNotificationManager.CHANNEL_COMPLETE,
                        "Download Complete",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Notifies when a download finishes" },

                    NotificationChannel(
                        DownloadNotificationManager.CHANNEL_ERROR,
                        "Download Errors",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Notifies when a download fails" },

                    NotificationChannel(
                        DownloadNotificationManager.CHANNEL_SERVICE,
                        "Download Service",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "Persistent service notification" }
                )
            )
        }
    }
}
