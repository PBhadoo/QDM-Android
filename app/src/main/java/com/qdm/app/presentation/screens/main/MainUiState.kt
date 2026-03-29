package com.parveenbhadoo.qdm.presentation.screens.main

import com.parveenbhadoo.qdm.R
import com.parveenbhadoo.qdm.domain.model.DownloadItem
import com.parveenbhadoo.qdm.domain.model.DownloadState

data class MainUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val activeTab: DownloadTab = DownloadTab.ALL,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val showAddSheet: Boolean = false,
    val prefillUrl: String = "",
    val showFolderSetupDialog: Boolean = false,
    val propertiesItem: DownloadItem? = null,
    val copyMoveRenameItem: DownloadItem? = null
) {
    val filteredDownloads: List<DownloadItem>
        get() = downloadsForTab(activeTab)

    fun downloadsForTab(tab: DownloadTab): List<DownloadItem> {
        val byTab = when (tab) {
            DownloadTab.ALL -> downloads
            DownloadTab.DOWNLOADING -> downloads.filter {
                it.state is DownloadState.Downloading || it.state is DownloadState.Connecting
            }
            DownloadTab.PENDING -> downloads.filter {
                it.state is DownloadState.Pending && !it.isQueued
            }
            DownloadTab.QUEUE -> downloads.filter {
                it.state is DownloadState.Pending && it.isQueued
            }
            DownloadTab.FINISHED -> downloads.filter { it.state is DownloadState.Completed }
            DownloadTab.ERROR -> downloads.filter { it.state is DownloadState.Error }
            DownloadTab.SCHEDULED -> downloads.filter { it.state is DownloadState.Scheduled }
        }
        return if (searchQuery.isBlank()) byTab
        else byTab.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
    }
}

enum class DownloadTab(val labelRes: Int) {
    ALL(R.string.tab_all),
    DOWNLOADING(R.string.tab_downloading),
    PENDING(R.string.tab_pending),
    QUEUE(R.string.tab_queue),
    FINISHED(R.string.tab_finished),
    ERROR(R.string.tab_error),
    SCHEDULED(R.string.tab_scheduled)
}
