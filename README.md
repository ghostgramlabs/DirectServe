<h1 align="center">DirectServe</h1>

<p align="center">
  Share files and stream media from your Android phone to any browser on the same Wi-Fi.<br>
  No app on the other device, no cloud, no cables, no account.
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.ghostgramlabs.directserve">Google Play</a> ·
  <a href="https://ghostgramlabs.com/DirectServe/">Website</a> ·
  <a href="https://ghostgramlabs.com/directserve-privacy/">Privacy policy</a> ·
  <a href="LICENSE">Apache 2.0</a>
</p>

<p align="center">
  <a href="https://github.com/ghostgramlabs/DirectServe/actions/workflows/ci.yml"><img src="https://github.com/ghostgramlabs/DirectServe/actions/workflows/ci.yml/badge.svg" alt="CI status"></a>
</p>

DirectServe runs a small web server on your phone. Open the link or scan the QR code on an
iPhone, PC, Mac, tablet, Chromebook or smart TV, and browse, stream and download what you chose
to share, straight over your local network.

## Features

- **Browser-based sharing.** Videos, photos, music and any other file type, served to any
  modern browser on the same Wi-Fi or hotspot.
- **Smart compatibility.** When a browser can't play a video or audio format, DirectServe
  remuxes, transmuxes or transcodes it on the phone, on demand only, to save battery.
- **Uploads to the phone.** Send files from the other device back to the phone.
- **Live screen mirroring.** Mirror the phone's screen to a browser over WebRTC, optionally
  with device audio or microphone, and protected by a PIN.
- **Smart TVs.** DLNA/UPnP streaming to TVs and players such as VLC and Kodi.
- **Quick text.** Share text and clipboard snippets between devices.
- **Local-only and private.** Nothing passes through a cloud server. Sessions can be PIN
  protected.

## Building

Requirements: Android Studio Narwhal or newer (Android Gradle Plugin 8.11), JDK 17, Android SDK 36.

```bash
git clone https://github.com/ghostgramlabs/DirectServe.git
cd DirectServe
./gradlew :app:assembleDebug      # Windows: gradlew.bat :app:assembleDebug
./gradlew testDebugUnitTest       # unit tests for every module
```

Debug builds need no setup and install alongside the Play version (they use a `.debug`
application ID suffix).

### Release signing

Release builds read their key from `keystore.properties` in the project root, which git
ignores:

```properties
storeFile=your-release-key.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Without that file, release builds are left unsigned. Never commit a keystore or its passwords.

## Project structure

DirectServe is a multi-module Gradle project in Kotlin, using Jetpack Compose for the app UI,
Ktor for the embedded server and Media3 for media conversion.

| Module | Responsibility |
| --- | --- |
| `:app` | Application, foreground service, server lifecycle, top-level UI |
| `:core:network` | Ktor HTTP server, byte-range and HLS streaming, DLNA, local discovery |
| `:core:media` | Media analysis, playback decision engine, remux/transmux/transcode pipeline |
| `:core:storage`, `:core:history`, `:core:session`, `:core:settings`, `:core:model` | Shared data, sessions and settings |
| `:core:resources` | Strings, translations and shared resources |
| `:feature:*` | Compose screens: home, library, session, settings, onboarding, network setup, history |
| `:webassets` | The browser interface served to other devices (HTML, CSS, JS) |

More detail:

- [docs/repo-structure.md](docs/repo-structure.md): module map and data/control flow
- [docs/media-pipeline.md](docs/media-pipeline.md): how media is analysed and converted
- [docs/playback-reliability.md](docs/playback-reliability.md): playback rules and fallbacks
- [docs/test-matrix.md](docs/test-matrix.md): devices and browsers to test against
- [docs/directserve_manual.md](docs/directserve_manual.md): full technical and functional manual
- [CLAUDE.md](CLAUDE.md) and [AGENTS.md](AGENTS.md): the architectural rules, written for AI
  coding assistants but useful for anyone. The main one: the server never does work the user
  didn't ask for.

The browser interface bundles hls.js, Plyr and Uppy under their own licenses; see
[NOTICE](NOTICE).

## Contributing

Bug reports, translations and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md)
and the [code of conduct](CODE_OF_CONDUCT.md). To report a security problem, follow
[SECURITY.md](SECURITY.md) instead of opening a public issue. Changes are listed in
[CHANGELOG.md](CHANGELOG.md).

## License

DirectServe is released under the [Apache License 2.0](LICENSE). You may use, modify and
redistribute it, including commercially, as long as you keep the copyright notice and the
[NOTICE](NOTICE) file, which credits GhostGram Labs.

The DirectServe name and icon are not covered by the license. If you publish a fork, give it its
own name, icon and application ID.

Made by [GhostGram Labs](https://ghostgramlabs.com).
