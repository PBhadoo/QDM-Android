package com.qdm.app.domain.usecase

import com.qdm.app.data.repository.DownloadRepository
import com.qdm.app.domain.engine.DownloadEngine
import com.qdm.app.domain.model.DownloadState
import javax.inject.Inject

class CancelDownloadUseCase @Inject constructor(
    private val engine: DownloadEngine,
    private val repository: DownloadRepository
) {
    suspend fun execute(id: String, deleteFile: Boolean = false) {
        engine.cancelDownload(id)
        if (deleteFile) {
            repository.deleteDownload(id)
        } else {
            repository.updateState(id, DownloadState.Cancelled)
        }
    }
}
