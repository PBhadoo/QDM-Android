package com.parveenbhadoo.qdm.domain.usecase

import android.content.Context
import android.content.Intent
import com.parveenbhadoo.qdm.service.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ResumeDownloadUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun execute(id: String) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, id)
        }
        context.startForegroundService(intent)
    }
}
