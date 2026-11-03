# Changelog

User-facing changes to DirectServe, newest first. Version numbers match the Google Play release
(`versionName` in `app/build.gradle.kts`).

## 1.0.27 - 2026-11-03

- A simpler app: choose what to share, tap Start, and scan the QR code. Settings are grouped,
  with expert options under Advanced.
- New teal look and app icon.
- Select all when adding from your phone library or removing shared items.
- DirectServe may ask for a rating after a couple of successful shares (at most once every few
  months).
- Redesigned browser pages: clearer cards with one Play button, a dark video and photo viewer,
  and simpler Live Screen and Send text pages.
- Fixed: reloading the Send or Folders page in the browser showed "not found".
- Shorter welcome tour (3 pages instead of 8), and simpler Mirror screen, Send text and History
  screens.
- Mirror screen sound stays in step with the picture instead of drifting behind it.
- Fixed: Live Screen could crash when a viewer refreshed the page.
- Fixed: History showed the wrong "nothing here yet" message on each tab.
- DirectServe is now open source under the Apache License 2.0.
- Long sharing sessions no longer stop or crash after a few hours on Android 15 and newer.
- Fixed: a crash when another app was already using DirectServe's port. Sharing now picks a
  free port automatically.
- Lower memory use when showing previews of large photos.
- Smaller, faster app download.

## 1.0.26 - 2026-08-30

- Android 16 support.
- Simpler sharing links.

## 1.0.25 - 2026-05-05

- The browser interface is available in 26 languages.
- Better playback on iPhone and iPad: playback waits until a converted video is ready, and
  HEVC videos are converted to an iOS-safe format.
- Fixed: VP9 playback on Fire OS tablets, original-quality playback in browsers, and the HLS
  fallback on TVs.
- Smoother library search in the browser, and a visibly spinning preparation indicator.
- Fixed: splash screen icon mismatches on startup.
- Three crash fixes.

## 1.0.23 - 2026-04-28

- Give your device a nickname.
- Quick text reaches the right browser, and messages are limited to a sensible length.
- Fixed: flickering WebM/VP9 playback and a preparation loop in the browser.
- Back works with a single press from the active session.
- History records are debounced, and the history tabs are clearer.
- Updated privacy policy to match the app's permissions.

## Earlier versions

Releases before 1.0.23 (April 2026) are recorded in the git history.
