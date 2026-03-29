package com.parveenbhadoo.qdm.domain.usecase

import com.parveenbhadoo.qdm.data.repository.DownloadRepository
import com.parveenbhadoo.qdm.domain.model.AddDownloadRequest
import javax.inject.Inject

class AddDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend fun execute(request: AddDownloadRequest, isQueued: Boolean = false): String =
        repository.addDownload(request, isQueued)
}
