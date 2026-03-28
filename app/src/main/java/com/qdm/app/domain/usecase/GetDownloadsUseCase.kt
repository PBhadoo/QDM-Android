package com.parveenbhadoo.qdm.domain.usecase

import com.parveenbhadoo.qdm.data.repository.DownloadRepository
import com.parveenbhadoo.qdm.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    fun execute(): Flow<List<DownloadItem>> = repository.getAllDownloads()
}
