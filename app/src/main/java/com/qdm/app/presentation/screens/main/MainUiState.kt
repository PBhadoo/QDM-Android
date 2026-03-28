package com.qdm.app.presentation.screens.main

import com.qdm.app.R
import com.qdm.app.domain.model.DownloadItem
import com.qdm.app.domain.model.DownloadState

data class MainUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val activeTab: DownloadTab = DownloadTab.ALL,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val showAddSheet: Boolean = false,
    val prefillUrl: String = ""
) {
    val filteredDownloads: List<DownloadItem>
        get() {
            val byTab = when (activeTab) {
                DownloadTab.ALL -> downloads
                DownloadTab.DOWNLOADING -> downloads.filter {
                    it.state is DownloadState.Downloading || it.state is DownloadState.Connecting
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
    FINISHED(R.string.tab_finished),
    ERROR(R.string.tab_error),
    SCHEDULED(R.string.tab_scheduled)
}
