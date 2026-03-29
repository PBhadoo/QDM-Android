package com.parveenbhadoo.qdm.presentation.screens.browser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parveenbhadoo.qdm.data.local.entity.BookmarkEntity
import com.parveenbhadoo.qdm.data.local.entity.BrowserHistoryEntity
import com.parveenbhadoo.qdm.data.preferences.UserPreferencesDataStore
import com.parveenbhadoo.qdm.data.repository.BookmarkRepository
import com.parveenbhadoo.qdm.data.repository.BrowserRepository
import com.parveenbhadoo.qdm.utils.QdmLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject

// --- Ad Block filter sources ---

data class AdBlockSource(val id: String, val name: String, val url: String)

data class AdBlockSourceState(
    val source: AdBlockSource,
    val enabled: Boolean,
    val hostCount: Int = 0,
    val lastUpdated: Long = 0L,
    val isUpdating: Boolean = false,
    val error: String? = null
)

val DEFAULT_ADBLOCK_SOURCES = listOf(
    AdBlockSource("stevenblack", "StevenBlack Combined", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"),
    AdBlockSource("easylist", "EasyList", "https://easylist.to/easylist/easylist.txt"),
    AdBlockSource("ublock_filters", "uBlock Filters", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"),
    AdBlockSource("adguard_base", "AdGuard Base Filter", "https://filters.adtidy.org/extension/chromium/filters/2.txt"),
    AdBlockSource("easyprivacy", "EasyPrivacy", "https://easylist.to/easylist/easyprivacy.txt"),
    AdBlockSource("peter_lowe", "Peter Lowe's Ad and Tracking", "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext"),
    AdBlockSource("abpindo", "ABPindo Indonesian Filter", "https://raw.githubusercontent.com/ABPindo/indonesianadblockrules/master/subscriptions/abpindo.txt"),
    AdBlockSource("adblock_warning", "AdBlock Warning Removal", "https://easylist-downloads.adblockplus.org/antiadblockfilters.txt"),
    AdBlockSource("ublock_unbreak", "uBlock Filters — Unbreak", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/unbreak.txt"),
    AdBlockSource("adguard_mobile", "AdGuard Mobile Ads Filter", "https://filters.adtidy.org/extension/chromium/filters/11.txt"),
    AdBlockSource("ublock_badware", "uBlock Filters — Badware Risks", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt"),
    AdBlockSource("ublock_privacy", "uBlock Filters — Privacy", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt"),
    AdBlockSource("ublock_resource", "uBlock Filters — Resource Abuse", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/resource-abuse.txt"),
    AdBlockSource("adguard_annoyances", "AdGuard Annoyances", "https://filters.adtidy.org/extension/chromium/filters/14.txt"),
    AdBlockSource("fanboy_annoyance", "Fanboy's Annoyance List", "https://easylist.to/easylist/fanboy-annoyance.txt"),
    AdBlockSource("ublock_annoyances", "uBlock Annoyances", "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt"),
    AdBlockSource("fanboy_turkish", "Fanboy's Turkish Filter", "https://www.fanboy.co.nz/fanboy-turkish.txt"),
    AdBlockSource("adguard_turkish", "AdGuard Turkish Filter", "https://filters.adtidy.org/extension/chromium/filters/13.txt"),
    AdBlockSource("adguard_russian", "AdGuard Russian Filter", "https://filters.adtidy.org/extension/chromium/filters/1.txt"),
    AdBlockSource("adguard_spanish", "AdGuard Spanish/Portuguese Filter", "https://filters.adtidy.org/extension/chromium/filters/3.txt"),
    AdBlockSource("easylist_german", "EasyList German Filter", "https://easylist.to/easylistgermany/easylistgermany.txt"),
    AdBlockSource("oisd_full", "OISD ABP Full", "https://abp.oisd.nl/")
)

// --- UI State ---

data class BrowserUiState(
    val currentUrl: String = "https://www.google.com",
    val pageTitle: String = "",
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val detectedMediaUrl: String? = null,
    val detectedMediaHeaders: Map<String, String> = emptyMap(),
    val adBlockedCount: Int = 0,
    val showHistory: Boolean = false,
    val showBrowserSettings: Boolean = false,
    val showAdBlockManager: Boolean = false,
    val showBrowserInfo: Boolean = false,
    val desktopMode: Boolean = false,
    val javaScriptEnabled: Boolean = true,
    val loadImages: Boolean = true,
    val adBlockEnabled: Boolean = true,
    val filterSources: List<AdBlockSourceState> = emptyList(),
    // Tab support
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val activeTabIndex: Int = 0,
    val showTabPanel: Boolean = false,
    val showBookmarks: Boolean = false
) {
    val adBlockHostCount: Int get() = filterSources.filter { it.enabled }.sumOf { it.hostCount }
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val browserRepository: BrowserRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val stateHolder: BrowserStateHolder,
    private val prefs: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BrowserUiState(
            currentUrl = stateHolder.activeTab.url,
            tabs = stateHolder.tabs.toList(),
            activeTabIndex = stateHolder.activeTabIndex
        )
    )
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    val startUrl: String get() = stateHolder.activeTab.url

    private val _adBlockHosts = MutableStateFlow<Set<String>>(emptySet())
    val adBlockHosts: StateFlow<Set<String>> = _adBlockHosts.asStateFlow()

    init {
        // Load persisted adblock enabled state
        viewModelScope.launch {
            val enabled = prefs.adBlockEnabledFlow().first()
            _uiState.update { it.copy(adBlockEnabled = enabled) }
        }
        loadAllFilterSources()
    }

    // --- Filter source file helpers ---

    private fun sourceFile(id: String): File = File(context.filesDir, "adblock_$id.txt")

    private fun readSourceFile(id: String): Set<String> {
        val f = sourceFile(id)
        if (!f.exists()) return emptySet()
        return try {
            parseFilterContent(f.readText())
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun readSourceMeta(id: String): Pair<Int, Long> {
        val f = sourceFile(id)
        if (!f.exists()) return Pair(0, 0L)
        val hosts = try { parseFilterContent(f.readText()) } catch (e: Exception) { emptySet() }
        return Pair(hosts.size, f.lastModified())
    }

    /** Parse hosts-format and ABP-format filter lists into a set of hostnames. */
    fun parseFilterContent(content: String): Set<String> =
        content.lines()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("!") && !it.startsWith("[") }
            .mapNotNull { line ->
                when {
                    line.startsWith("0.0.0.0 ") || line.startsWith("127.0.0.1 ") ->
                        line.split(Regex("\\s+")).getOrNull(1)
                            ?.takeIf { it != "localhost" && it != "0.0.0.0" && it.contains(".") }
                    line.startsWith("||") ->
                        line.removePrefix("||").substringBefore("^").substringBefore("/")
                            .takeIf { it.isNotBlank() && !it.contains("*") && it.contains(".") }
                    !line.contains(" ") && line.contains(".") && !line.startsWith("/") && !line.startsWith("@") -> line
                    else -> null
                }
            }.toHashSet()

    private fun loadAllFilterSources() {
        viewModelScope.launch {
            // Load persisted enabled source IDs
            val savedSources = prefs.adBlockSourcesFlow().first()
            val enabledIds: Set<String> = if (savedSources.isBlank()) {
                // Default: only stevenblack enabled (fast to load, comprehensive)
                setOf("stevenblack")
            } else {
                savedSources.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            }

            // Build source state list
            val sourceStates = DEFAULT_ADBLOCK_SOURCES.map { source ->
                val enabled = enabledIds.contains(source.id)
                val (count, lastUpdated) = if (enabled) readSourceMeta(source.id) else Pair(0, 0L)
                AdBlockSourceState(source = source, enabled = enabled, hostCount = count, lastUpdated = lastUpdated)
            }
            _uiState.update { it.copy(filterSources = sourceStates) }

            // If no cached file for stevenblack, fall back to bundled asset
            if (!sourceFile("stevenblack").exists()) {
                try {
                    val hosts = context.assets.open("adblock_hosts.txt").bufferedReader().useLines { lines ->
                        lines.map { it.trim().lowercase() }
                            .filter { it.isNotBlank() && !it.startsWith("#") }
                            .toHashSet()
                    }
                    _adBlockHosts.value = hosts
                    val updated = sourceStates.map { s ->
                        if (s.source.id == "stevenblack") s.copy(hostCount = hosts.size) else s
                    }
                    _uiState.update { it.copy(filterSources = updated) }
                    QdmLog.i("BrowserViewModel", "Bundled adblock hosts loaded: ${hosts.size}")
                    return@launch
                } catch (e: Exception) {
                    QdmLog.w("BrowserViewModel", "Bundled adblock load failed: ${e.message}")
                }
            }

            rebuildMergedHosts(sourceStates)
        }
    }

    private fun rebuildMergedHosts(sources: List<AdBlockSourceState>) {
        viewModelScope.launch(Dispatchers.IO) {
            val merged = HashSet<String>()
            sources.filter { it.enabled }.forEach { s ->
                merged.addAll(readSourceFile(s.source.id))
            }
            _adBlockHosts.value = merged
            QdmLog.i("BrowserViewModel", "Merged adblock hosts: ${merged.size}")
        }
    }

    fun toggleAdBlock() {
        val newVal = !_uiState.value.adBlockEnabled
        _uiState.update { it.copy(adBlockEnabled = newVal) }
        viewModelScope.launch { prefs.updateAdBlockEnabled(newVal) }
    }

    fun toggleFilterSource(id: String) {
        val current = _uiState.value.filterSources
        val updated = current.map { s ->
            if (s.source.id == id) s.copy(enabled = !s.enabled) else s
        }
        _uiState.update { it.copy(filterSources = updated) }
        viewModelScope.launch {
            val enabledIds = updated.filter { it.enabled }.map { it.source.id }.toSet()
            prefs.updateAdBlockSources(enabledIds)
            rebuildMergedHosts(updated)
        }
    }

    fun updateFilterSource(id: String) {
        val source = DEFAULT_ADBLOCK_SOURCES.find { it.id == id } ?: return
        val current = _uiState.value.filterSources
        _uiState.update {
            it.copy(filterSources = current.map { s ->
                if (s.source.id == id) s.copy(isUpdating = true, error = null) else s
            })
        }
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    URL(source.url).openStream().bufferedReader().readText()
                }
                val parsed = parseFilterContent(content)
                withContext(Dispatchers.IO) {
                    sourceFile(id).writeText(parsed.joinToString("\n"))
                }
                val updatedSources = _uiState.value.filterSources.map { s ->
                    if (s.source.id == id) s.copy(
                        isUpdating = false,
                        hostCount = parsed.size,
                        lastUpdated = System.currentTimeMillis(),
                        error = null
                    ) else s
                }
                _uiState.update { it.copy(filterSources = updatedSources) }
                rebuildMergedHosts(updatedSources)
                QdmLog.i("BrowserViewModel", "Updated filter $id: ${parsed.size} hosts")
            } catch (e: Exception) {
                QdmLog.e("BrowserViewModel", "Filter update failed for $id: ${e.message}")
                _uiState.update {
                    it.copy(filterSources = it.filterSources.map { s ->
                        if (s.source.id == id) s.copy(isUpdating = false, error = e.message ?: "Update failed") else s
                    })
                }
            }
        }
    }

    fun updateAllFilterSources() {
        _uiState.value.filterSources.filter { it.enabled }.forEach { updateFilterSource(it.source.id) }
    }

    fun setAllFilterSources(enabled: Boolean) {
        val updated = _uiState.value.filterSources.map { it.copy(enabled = enabled) }
        _uiState.update { it.copy(filterSources = updated) }
        viewModelScope.launch {
            val ids = if (enabled) updated.map { it.source.id }.toSet() else emptySet()
            prefs.updateAdBlockSources(ids)
            if (enabled) rebuildMergedHosts(updated) else _adBlockHosts.value = emptySet()
        }
    }

    // --- AdBlock Manager UI ---
    fun showAdBlockManager() = _uiState.update { it.copy(showAdBlockManager = true) }
    fun dismissAdBlockManager() = _uiState.update { it.copy(showAdBlockManager = false) }

    // --- Browser Info ---
    fun showBrowserInfo() = _uiState.update { it.copy(showBrowserInfo = true) }
    fun dismissBrowserInfo() = _uiState.update { it.copy(showBrowserInfo = false) }

    // --- Page events ---
    fun onPageStarted(url: String) {
        _uiState.update { it.copy(currentUrl = url, progress = 0) }
        stateHolder.updateActiveTab(url = url)
    }

    fun onPageFinished(url: String, title: String) {
        _uiState.update { it.copy(currentUrl = url, pageTitle = title, progress = 100) }
        stateHolder.updateActiveTab(url = url, title = title.ifBlank { url })
        _uiState.update { it.copy(tabs = stateHolder.tabs.toList()) }
        viewModelScope.launch { browserRepository.addHistory(url, title) }
        refreshBookmarkState(url)
    }

    fun onProgressChanged(progress: Int) = _uiState.update { it.copy(progress = progress) }

    fun onNavigationStateChanged(canBack: Boolean, canForward: Boolean) =
        _uiState.update { it.copy(canGoBack = canBack, canGoForward = canForward) }

    fun onMediaDetected(url: String, headers: Map<String, String>) =
        _uiState.update { it.copy(detectedMediaUrl = url, detectedMediaHeaders = headers) }

    fun dismissMedia() = _uiState.update { it.copy(detectedMediaUrl = null, detectedMediaHeaders = emptyMap()) }

    fun onAdBlocked() = _uiState.update { it.copy(adBlockedCount = it.adBlockedCount + 1) }

    fun setUrl(url: String) = _uiState.update { it.copy(currentUrl = url) }

    // --- Bookmarks ---
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private fun refreshBookmarkState(url: String) {
        viewModelScope.launch {
            _isBookmarked.value = bookmarkRepository.isBookmarked(url)
        }
    }

    fun toggleBookmark() {
        val url = _uiState.value.currentUrl
        val title = _uiState.value.pageTitle.ifBlank { url }
        viewModelScope.launch {
            if (_isBookmarked.value) {
                bookmarkRepository.remove(url)
                _isBookmarked.value = false
            } else {
                bookmarkRepository.add(url, title)
                _isBookmarked.value = true
            }
        }
    }

    // --- Bookmarks list ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun showBookmarks() = _uiState.update { it.copy(showBookmarks = true) }
    fun dismissBookmarks() = _uiState.update { it.copy(showBookmarks = false) }
    fun deleteBookmark(url: String) { viewModelScope.launch { bookmarkRepository.remove(url) } }

    // --- History ---
    val history: StateFlow<List<BrowserHistoryEntity>> = browserRepository.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun showHistory() = _uiState.update { it.copy(showHistory = true) }
    fun dismissHistory() = _uiState.update { it.copy(showHistory = false) }
    fun clearHistory() { viewModelScope.launch { browserRepository.clearHistory() } }
    suspend fun searchHistory(query: String): List<BrowserHistoryEntity> = browserRepository.search(query)

    // --- Browser settings ---
    fun showBrowserSettings() = _uiState.update { it.copy(showBrowserSettings = true) }
    fun dismissBrowserSettings() = _uiState.update { it.copy(showBrowserSettings = false) }
    fun toggleDesktopMode() = _uiState.update { it.copy(desktopMode = !it.desktopMode) }
    fun toggleJavaScript() = _uiState.update { it.copy(javaScriptEnabled = !it.javaScriptEnabled) }
    fun toggleLoadImages() = _uiState.update { it.copy(loadImages = !it.loadImages) }

    // --- Tab management ---
    fun showTabPanel() = _uiState.update { it.copy(showTabPanel = true) }
    fun dismissTabPanel() = _uiState.update { it.copy(showTabPanel = false) }

    fun openNewTab(url: String = "https://www.google.com") {
        val tab = BrowserTab(url = url, title = "New Tab")
        stateHolder.tabs.add(tab)
        stateHolder.activeTabIndex = stateHolder.tabs.size - 1
        _uiState.update { it.copy(tabs = stateHolder.tabs.toList(), activeTabIndex = stateHolder.activeTabIndex, currentUrl = url) }
    }

    fun closeTab(index: Int) {
        if (stateHolder.tabs.size <= 1) {
            // Last tab — reset to blank
            stateHolder.tabs[0] = BrowserTab()
            stateHolder.activeTabIndex = 0
        } else {
            stateHolder.tabs.removeAt(index)
            stateHolder.activeTabIndex = (index - 1).coerceAtLeast(0).coerceAtMost(stateHolder.tabs.size - 1)
        }
        val newActive = stateHolder.activeTab
        _uiState.update {
            it.copy(
                tabs = stateHolder.tabs.toList(),
                activeTabIndex = stateHolder.activeTabIndex,
                currentUrl = newActive.url
            )
        }
    }

    fun switchTab(index: Int) {
        if (index < 0 || index >= stateHolder.tabs.size) return
        stateHolder.activeTabIndex = index
        val tab = stateHolder.tabs[index]
        _uiState.update { it.copy(activeTabIndex = index, currentUrl = tab.url) }
    }

    fun closeAllTabs() {
        stateHolder.tabs.clear()
        stateHolder.tabs.add(BrowserTab())
        stateHolder.activeTabIndex = 0
        _uiState.update {
            it.copy(
                tabs = stateHolder.tabs.toList(),
                activeTabIndex = 0,
                currentUrl = "https://www.google.com"
            )
        }
    }
}
