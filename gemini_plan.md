# Project: Advanced Android Download Manager (1DM Clone)
**Target:** AI Coding Assistant (Claude)
**Status:** ✅ Complete — v1.0.0
**Note:** Torrent features NOT implemented. Focus on modern Android best practices.

## Project Architecture & Tech Stack (Modern Android)
* **Language:** Kotlin 2.2.10
* **UI Toolkit:** Jetpack Compose (Material 3) with Compose BOM 2024.12.01
* **Architecture:** MVI/MVVM with Unidirectional Data Flow (UDF)
* **DI:** Hilt 2.56.1 with KSP 2.2.10-1.0.29
* **Async/Networking:** Kotlin Coroutines, StateFlow, OkHttp 4.12.0
* **Local Database:** Room 2.7.1 with KSP
* **Background Tasks:** Foreground Services (`dataSync`) + WorkManager 2.10.0
* **Storage:** SAF (Storage Access Framework) + MediaStore API
* **Settings:** DataStore Preferences 1.1.4
* **Localization:** Android 13+ LocaleManager + AppCompat fallback

---

## 🛠 Phase 0: Repository Scaffolding
- [x] `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- [x] `gradle/wrapper/gradle-wrapper.properties` (Gradle 9.3.1)
- [x] `.gitignore`
- [x] `.github/workflows/build.yml` (CI — builds & releases on tag push)
- [x] `app/build.gradle.kts` with all dependencies + signing config
- [x] `app/proguard-rules.pro`

## 🛠 Phase 1: Initial Setup & Permissions Architecture
- [x] `AndroidManifest.xml` — Internet, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, POST_NOTIFICATIONS, WAKE_LOCK
- [x] `res/values/strings.xml`, `themes.xml`
- [x] `res/xml/locale_config.xml`, `file_paths.xml`
- [x] Launcher icons (adaptive icon — blue background + download arrow)
- [x] Room entities: `DownloadEntity`, `BrowserHistoryEntity`, `ScheduledDownloadEntity`
- [x] DAOs: `DownloadDao`, `BrowserHistoryDao`, `ScheduledDownloadDao`
- [x] `QdmDatabase`
- [x] Domain models: `DownloadState` (sealed), `DownloadItem`, `AppSettings`, `FileMetadata`, `AddDownloadRequest`
- [x] `UserPreferencesDataStore` (DataStore-backed settings)
- [x] Repositories: `DownloadRepository`, `BrowserRepository`, `SettingsRepository`
- [x] Hilt modules: `AppModule`, `DatabaseModule`, `NetworkModule`
- [x] `QdmApplication` (@HiltAndroidApp + notification channels)
- [x] Theme: `QdmTheme`, `Color.kt`, `Type.kt` (Material 3, dynamic color)

## ⚙️ Phase 2: Core Download Engine (Network & IO)
- [x] `HttpClientProvider` — OkHttp singleton with HTTP/2, interceptors, connection pool
- [x] Interceptors: `HeaderInterceptor`, `CookieInterceptor`, `UserAgentInterceptor`
- [x] `FetchFileMetadataUseCase` — HEAD request (405 fallback to GET Range)
- [x] `SpeedCalculator` — rolling 3-second window
- [x] `ChunkDownloader` — Range request, FileChannel seek, 512KB progress interval
- [x] `DownloadEngine` — orchestrator with ConcurrentHashMap<Job>, supervisorScope, StateFlow
- [x] Use cases: `AddDownload`, `GetDownloads`, `Pause`, `Resume`, `Cancel`
- [x] `DownloadService` (LifecycleService, Hilt, START_STICKY, WakeLock + WifiLock)
- [x] `DownloadNotificationManager` — 4 channels, 1Hz throttle
- [x] `ScheduledDownloadWorker` (@HiltWorker)
- [x] Utils: `FormatUtils`, `MimeTypeHelper`, `NetworkUtils`, `FileUtils`

## 📱 Phase 3: Main Screen UI (Jetpack Compose)
- [x] `NavGraph` + `Screen` sealed class
- [x] `MainUiState` + `DownloadTab` enum
- [x] `MainViewModel` (combine DB + live StateFlow)
- [x] `MainScreen` — ModalNavigationDrawer + Scaffold + ScrollableTabRow
- [x] `DownloadItemRow` — MIME icon, progress bar, speed/ETA, 3-dot menu
- [x] `SpeedEtaText` composable
- [x] `MultiFab` — animated expand with 3 sub-actions
- [x] `MainActivity` — edge-to-edge, notification permission, intent handling

## ➕ Phase 4: Intent Handling & "Add Link" Flow
- [x] `AddDownloadUiState` — full state machine (URL → Fetch → Configured → Advanced)
- [x] `AddDownloadViewModel` — fetches metadata, builds request, enqueues download
- [x] `AddDownloadSheet` — ModalBottomSheet, SAF folder picker, thread slider, auth fields
- [x] Intent filters in Manifest: ACTION_SEND (text/plain), ACTION_VIEW (http/https)
- [x] URL extraction with `Patterns.WEB_URL` from shared text

## 🌐 Phase 5: Integrated Web Browser & Media Grabber
- [x] `WebViewWrapper` — AndroidView, DisposableEffect cleanup, settings configured
- [x] Media detection in `shouldInterceptRequest` — video/audio/PDF patterns
- [x] Ad blocker — `HashSet<String>` loaded from `assets/adblock_hosts.txt`
- [x] `BrowserViewModel` — history, ad block count, media detection state
- [x] `BrowserScreen` — URL bar, back/forward/refresh, progress indicator, media Snackbar
- [x] Cookie sync helper in BrowserScreen

## 🎛 Phase 6: Settings, Drawer & Theming
- [x] `SettingsViewModel` — all settings delegated to DataStore
- [x] Per-app language: Android 13+ LocaleManager + AppCompat fallback
- [x] `SettingsScreen` — sections: Downloads, Network, Appearance, Notifications, About
- [x] SAF folder picker in settings
- [x] Dynamic theme cycling (System → Light → Dark)
- [x] Navigation drawer in MainScreen

## 🔔 Phase 7: Polish, Notifications & Lifecycle
- [x] 4 notification channels created in `QdmApplication.onCreate()`
- [x] 1Hz notification throttle in `DownloadNotificationManager`
- [x] `POST_NOTIFICATIONS` runtime permission request in `MainActivity`
- [x] `START_STICKY` + null intent guard in `DownloadService`
- [x] WakeLock + WifiLock with safe release in `onDestroy()`
- [x] `collectAsStateWithLifecycle` used throughout (no `collectAsState`)
- [x] `key = { it.id }` on LazyColumn items

---

## CI / Release
- Tag `v1.0.0` → GitHub Actions → signs APK → creates GitHub Release
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
