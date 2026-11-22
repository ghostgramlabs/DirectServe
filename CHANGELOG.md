# Changelog

User-facing changes to DirectServe, newest first. Version numbers match the Google Play release
(`versionName` in `app/build.gradle.kts`).

## 1.0.29 - 2026-11-22

- Fixed: while only Mirror screen was on, a browser on the same Wi-Fi could read the mirroring
  PIN from the live status the page polls, and so open the mirror without being given the PIN.
  The PIN is no longer sent to browsers; viewers must type it.
- Settings > Privacy Policy now links to the full privacy policy and the terms of use, which
  open on ghostgramlabs.com.
- Sharing now asks for a PIN by default, so only people you give the PIN to can open your share,
  even on a busy Wi-Fi. You can still turn the PIN off in Settings.
- DirectServe no longer lets Android copy its data to your Google account backup or to a new
  phone. Settings, connected-device names and share history stay on this phone only.
- With notifications turned off for DirectServe on Android 13 or later, sharing notifications
  are now skipped safely instead of relying on the phone to ignore them.
- If the microphone permission is taken away while mirroring with sound, the sound stops with a
  "permission denied" status instead of a general failure.

## 1.0.28 - 2026-11-15

- Photos open full screen in the browser: swipe or use the arrow keys to browse, pinch or
  double-tap to zoom, a filmstrip of nearby photos, slideshow, details (with the date) and
  download. Next keeps going across pages, and one Back returns to the page you were on.
- Videos in the browser have Previous / Next, an Up next row, and offer the next video when one
  ends (it only starts when you choose it). Reopening a video says where it resumed, with Start
  over. On phones, double-tap the left or right side to skip 10 seconds.
- Sort any list by newest, oldest, name or size; the photo viewer and video player follow it.
- Files show as a clear list with a type badge, size, date, View and Download.
- Music plays as a queue: the next track starts by itself, with Previous / Next in the Now
  playing bar and on the lock screen.
- The browser says when it can't reach the phone and recovers by itself when it can again.
- Home and Back on every viewer, and a Retry / Home choice instead of a dead-end error page.
- Fixed: a video still being prepared stopped a few seconds in and never continued.
- Fixed: videos the browser can play as they are (such as WebM and AV1) sometimes failed to
  start the first time the browser opened the share.
- Fixed: files shared from a picked folder lost their date.
- Fixed: "Add everything on this phone" and the phone library left out PDFs, Word, Excel,
  PowerPoint and text files (and subtitles); only ZIPs, APKs and similar were added.
- Fixed: a file added both from a picked folder and from the phone library showed twice. It now
  shows once, and removing it removes both.
- Fixed: a file that briefly couldn't be read when the app started stayed "Unavailable" (and
  "no longer available" in the browser) even though it was there. The app now looks again
  shortly after starting, and the browser checks again before giving up.
- Fixed: the home page was empty right after entering the PIN.
- Fixed: some videos with known size lost player controls such as keyboard shortcuts.
- Faster start for videos that need converting: the phone reads the video where it is instead
  of first copying the whole file (minutes for a long film, and twice the storage).
- HDR10, HLG and Dolby Vision 8.x videos play directly in browsers that can show 10-bit HEVC
  (such as Chrome or Edge with hardware support) instead of waiting for a slow conversion.
- When a browser can't decode a video, the phone goes straight to converting it instead of
  first trying a repackage that could not help.
- Fixed: long videos over 2 GB never fell back to conversion when the browser couldn't play the
  original, and a video could stay on "Preparing" forever when nothing was being prepared.
- Videos being converted keep playing to the end while the phone works: playback continues as
  soon as about 12 more seconds are ready (it used to wait for 15% of the whole video), and
  when the browser can't use the streaming format it carries on from the same file instead of
  stopping.
- Picking another video while one is still converting starts the new one straight away when
  nobody is watching the first, instead of waiting for that whole conversion to finish.
- Fixed: a converted video could get stuck on an empty player when its conversion finished
  just as it was starting, and could replay its last moment over and over at the end.
- Fixed: thumbnails of other videos no longer slow down the start of the one you picked.
- Fixed: the photo slideshow did nothing when a photo was opened from Home or a link, moved on
  before a slow photo had loaded, and stopped at the last photo; it now starts over.
- The copy-address button on the sharing screen is a compact icon in every language.

## 1.0.27 - 2026-11-03

- A simpler app: choose what to share, tap Start, and scan the QR code. Settings are grouped,
  with expert options under Advanced.
- New ice-blue look and app icon.
- Mirror screen sound is now your choice: turn on Share sound before you start. Android only asks
  for the microphone permission when you do, and mirroring works without it.
- Select all when adding from your phone library or removing shared items.
- Simpler adding: Pick Files and Pick Folder open the picker straight away and add what you
  choose, with no extra confirm screen. "+ Add" lists every way to add, and you see how many
  items were added.
- While sharing, files you add or remove on the phone update in the browser on their own.
- New browser home in the style of a streaming app: a featured video with Play, then rows to swipe
  through (Continue watching, New videos, each shared folder, Photos, Music) with See all.
- Continue watching: the browser remembers where you stopped and Resume picks up from there.
- Video, photo and music pages use numbered pages (24 per page) instead of Load more.
- Back in the browser returns to the page you came from, scrolled where you left it.
- Wide videos turn the phone to landscape when played full screen in the browser.
- The share address always fits on one line, and menus and popups match the app's colours.
- DirectServe may ask for a rating after a couple of successful shares (at most once every few
  months).
- Redesigned browser pages: clearer cards with one Play button, a dark video and photo viewer,
  and simpler Live Screen and Send text pages.
- Fixed: a video picked from two Phone Library groups was shared twice.
- Fixed: shared folders and picked files showed 0 items after the app restarted.
- Videos that need converting start as soon as you press Play, without a second "Prepare" tap.
- Fixed: some MKV videos with Dolby (AC-3) sound played silently, and Ogg/Theora videos played
  sound with a blank picture.
- Converted videos keep their original size instead of being enlarged to 1080p, so they are ready
  sooner.
- Music the browser can't play now says so, with a download option.
- The video list opens much faster on large shares (about 1.5 s instead of 6 s for 40 videos, then
  instant).
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
