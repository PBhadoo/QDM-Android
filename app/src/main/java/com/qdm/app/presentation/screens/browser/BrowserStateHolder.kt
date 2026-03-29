package com.parveenbhadoo.qdm.presentation.screens.browser

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Survives NavController pop/push — holds the browser tab list and active index
 * so the browser restores its state when the user navigates back to BrowserScreen.
 */
@Singleton
class BrowserStateHolder @Inject constructor() {
    val tabs: MutableList<BrowserTab> = mutableListOf(BrowserTab())
    var activeTabIndex: Int = 0

    val activeTab: BrowserTab
        get() = tabs.getOrElse(activeTabIndex) { BrowserTab() }

    fun updateActiveTab(url: String? = null, title: String? = null) {
        if (tabs.isEmpty()) return
        val idx = activeTabIndex.coerceIn(0, tabs.size - 1)
        val current = tabs[idx]
        tabs[idx] = current.copy(
            url = url ?: current.url,
            title = title ?: current.title
        )
    }
}
