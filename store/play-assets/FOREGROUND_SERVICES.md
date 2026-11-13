# Foreground service declaration

Play Console → Policy and programs → App content → Foreground service permissions.

Answer **Yes**, the app uses foreground service permissions, and declare these three types.
They match `app/src/main/AndroidManifest.xml`; the merged release manifest adds no others.

| Type | Service | Started when |
|---|---|---|
| Connected device | `GhostStreamForegroundService` | The user taps Start sharing |
| Media projection | `LiveScreenCaptureService` | The user starts Mirror screen and accepts Android's screen-capture prompt |
| Microphone | `LiveScreenCaptureService` | Only when the user turns on **Share sound** before starting Mirror screen and grants Android's audio permission (from versionCode 30) |

Declare **all three** in the same declaration. Leaving one out is what Play reports as "Permission use
is either not declared or incorrectly declared".

Each type needs a task, a description and a link to a video. Upload the videos to YouTube as
Unlisted (or share them from Google Drive with "anyone with the link") and paste the links:

| Video | Use it for |
|---|---|
| `fgs_connected_device_demo.mp4` | Connected device |
| `fgs_mirror_sound_demo.mp4` | Media projection **and** Microphone (same link for both; recorded on versionCode 30) |

---

## Connected device (`FOREGROUND_SERVICE_CONNECTED_DEVICE`)

**Task to select:** Continuous data transfer to an external device (not "Automotive key").

**Description**

```
DirectServe turns the phone into a local media server so other devices on the same Wi-Fi or hotspot (a TV, laptop, tablet or phone) can browse, stream and download the files the user chose to share. The foreground service starts only when the user taps "Start sharing" and keeps the local web server reachable for the connected devices while the user switches apps or the screen turns off. A persistent notification shows that sharing is on, how many devices are connected, and a Stop button. The service stops when the user taps Stop.
```

**Impact if the task is deferred or interrupted**

```
Connected devices immediately lose access: videos stop mid-playback, downloads and uploads fail, and the TV or laptop shows the share as unavailable. The task cannot be deferred because the other device is actively requesting files in real time.
```

**Video:** `fgs_connected_device_demo.mp4` (65 s, 1080p, captioned). It shows Start sharing → the
notification with Stop → a laptop opening the share and playing a video → the user switching to
another app while the video keeps streaming → tapping Stop in the notification → the laptop losing
access. Upload it to YouTube as Unlisted (or share it from Google Drive) and paste that link.

---

## Media projection (`FOREGROUND_SERVICE_MEDIA_PROJECTION`)

**Task to select:** the screen sharing / projecting the screen to another device option.

**Description**

```
DirectServe's "Mirror screen" feature shows the phone's screen live in a web browser on another device on the same local network, for presentations, demos and showing photos to a room. It starts only when the user opens Mirror screen, taps Start and accepts Android's screen-capture prompt. The foreground service holds the MediaProjection session and streams the captured screen to the viewer's browser over the local network. Android's capture indicator and a DirectServe notification with a Stop button are shown for the whole session.
```

**Impact if the task is deferred or interrupted**

```
The live view on the other device freezes or goes black and the presentation stops. Mirroring must run continuously while the user uses other apps, so it cannot be deferred.
```

**Video:** `fgs_mirror_screen_demo.mp4` (83 s, captioned). The user opens Mirror screen and taps
Start → allows audio → chooses Share entire screen in Android's prompt → the red status-bar chip and
the "Screen sharing in progress" notification appear → a laptop browser shows the phone screen live
→ a video plays on the phone and on the laptop → the user taps Stop in the notification → the laptop
loses the view.

---

## Microphone (`FOREGROUND_SERVICE_MICROPHONE`)

Play rejected versionCode 28 for this type: "Your in-app experience or video does not match the
Microphone - Background Audio Input in your declaration" and "Use of FGS is not perceptible to the
user". In that build sound was captured on every mirroring session, with nothing on screen saying so.

From versionCode 30, sound is opt-in. The Mirror screen has a **Share sound** switch, off by default.
Only when the user turns it on does the app ask for the microphone permission and start the service
with the microphone type. The notification then reads "DirectServe is mirroring your screen and sound
to the browser". With the switch off the service runs as media projection only (checked on the
emulator: `types=0x20` off, `0xA0` on).

**Task to select:** the option for capturing or recording audio (Background audio input). Do not pick voice calls.

**Description**

```
Microphone use is opt-in. On the Mirror screen page the user turns on the "Share sound" switch (off by default), which explains that Android will ask for the microphone permission and show the microphone indicator. Only then does DirectServe request RECORD_AUDIO and run its mirroring foreground service with the microphone type, so the viewer's browser hears the sound other apps are playing (Android's AudioPlaybackCapture) together with the mirrored picture. The WebRTC audio pipeline opens the microphone only as a timing source; its samples are replaced with the captured playback sound, and no microphone audio is sent or stored. While sound is shared, Android's microphone indicator is on and the notification says "DirectServe is mirroring your screen and sound to the browser" with a Stop button. Sound capture ends when the user taps Stop. With the switch off, mirroring runs without the microphone type.
```

**Impact if the task is deferred or interrupted**

```
The mirrored picture continues but the viewer hears no sound, or sound drifts out of step with the picture. Audio must be captured in real time alongside the screen the user is sharing, so it cannot be deferred.
```

**Video:** `fgs_mirror_sound_demo.mp4` (99 s, captioned, recorded on the versionCode 30 build). It shows, in order:
1. The Mirror screen page with **Share sound** off, then the user turning it on (the switch text is readable).
2. Tapping Start → Android's "Allow DirectServe to record audio?" prompt → While using the app.
3. Android's screen-capture prompt → Entire screen → Start.
4. The status bar's microphone/privacy indicator, and the pulled-down notification reading
   "mirroring your screen and sound" with Stop.
5. A laptop browser showing the phone screen while a video plays on the phone, with its sound.
6. Tapping Stop in the notification: the indicator goes away and the laptop view ends.

