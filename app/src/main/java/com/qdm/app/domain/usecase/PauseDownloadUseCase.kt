package com.parveenbhadoo.qdm.domain.usecase

import com.parveenbhadoo.qdm.data.repository.DownloadRepository
import com.parveenbhadoo.qdm.domain.engine.DownloadEngine
import com.parveenbhadoo.qdm.domain.model.DownloadState
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
