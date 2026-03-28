package com.qdm.app.domain.usecase

import com.qdm.app.data.repository.DownloadRepository
import com.qdm.app.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    fun execute(): Flow<List<DownloadItem>> = repository.getAllDownloads()
}
