package com.qdm.app.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qdm.app.data.repository.DownloadRepository
import com.qdm.app.service.DownloadService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduledDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DownloadRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        val item = repository.getDownloadById(downloadId) ?: return Result.failure()

        val intent = Intent(applicationContext, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, item.id)
        }
        applicationContext.startForegroundService(intent)
        return Result.success()
    }
}
