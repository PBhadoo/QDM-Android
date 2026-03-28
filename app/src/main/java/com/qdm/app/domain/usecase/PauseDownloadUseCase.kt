package com.qdm.app.domain.usecase

import com.qdm.app.data.repository.DownloadRepository
import com.qdm.app.domain.engine.DownloadEngine
import com.qdm.app.domain.model.DownloadState
import javax.inject.Inject

class PauseDownloadUseCase @Inject constructor(
    private val engine: DownloadEngine,
    private val repository: DownloadRepository
) {
    suspend fun execute(id: String) {
        engine.pauseDownload(id)
        repository.updateState(id, DownloadState.Paused)
    }
}
