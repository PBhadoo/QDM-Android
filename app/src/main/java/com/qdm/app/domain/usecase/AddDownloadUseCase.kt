package com.qdm.app.domain.usecase

import com.qdm.app.data.repository.DownloadRepository
import com.qdm.app.domain.model.AddDownloadRequest
import javax.inject.Inject

class AddDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend fun execute(request: AddDownloadRequest): String = repository.addDownload(request)
}
