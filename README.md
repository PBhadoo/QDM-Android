<div align="center">

# QDM — Android Download Manager

A fast, modern download manager for Android built with Jetpack Compose and Material 3.

[![Build & Sign APK](https://github.com/PBhadoo/QDM-Android/actions/workflows/build.yml/badge.svg)](https://github.com/PBhadoo/QDM-Android/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/PBhadoo/QDM-Android)](https://github.com/PBhadoo/QDM-Android/releases/latest)
[![Min SDK](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://android-arsenal.com/api?level=26)

</div>

## Features

- **Parallel chunk downloading** — splits files into multiple chunks and downloads simultaneously for maximum speed
- **Integrated browser** — built-in WebView browser with automatic media/file detection
- **Ad blocking** — host-based ad blocking in the integrated browser
- **Cookie & header passthrough** — browser captures cookies, referer, and user-agent so downloads authenticate correctly
- **Resume support** — paused and interrupted downloads resume from where they left off
- **Scheduled downloads** — schedule downloads for a later time via WorkManager
- **Storage Access Framework** — saves files anywhere the user picks, including SD cards
- **Share-to-download** — share any URL from any app to instantly open the download dialog
- **Dynamic color** — supports Material You dynamic theming on Android 12+

## Screenshots

_Coming soon_

## Download

Grab the latest APK from the [Releases](https://github.com/PBhadoo/QDM-Android/releases/latest) page.

## Requirements

- Android 8.0 (API 26) or higher

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVI/MVVM — Unidirectional Data Flow |
| DI | Hilt |
| Database | Room |
| Networking | OkHttp |
| Preferences | DataStore |
| Background work | WorkManager |
| Image loading | Coil |

## Building

### Requirements

- Android Studio Meerkat or newer
- JDK 17+

### Steps

```bash
git clone https://github.com/PBhadoo/QDM-Android.git
cd QDM-Android
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Release build (signed)

Set the following environment variables before building:

```bash
export KEYSTORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
```

Place your `keystore.jks` in the `app/` directory, then:

```bash
./gradlew assembleRelease
```

## CI / Releases

Every push to `main` triggers a build. Pushing a `v*` tag (e.g. `v1.0.1`) triggers a full signed release build and publishes the APK to GitHub Releases automatically.

## Credits

QDM is inspired by the UX and feature set of:

- [IDM — Internet Download Manager](https://www.internetdownloadmanager.com/) (Windows)
- [1DM — Download Manager & Browser](https://play.google.com/store/apps/details?id=idm.internet.download.manager) (Android)

## License

```
Copyright 2026 Parveen Bhadoo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
