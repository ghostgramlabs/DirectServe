# Foreground service declaration

Play Console → Policy and programs → App content → Foreground service permissions.

Answer **Yes**, the app uses foreground service permissions, and declare these three types.
They match `app/src/main/AndroidManifest.xml`; the merged release manifest adds no others.

| Type | Service | Started when |
|---|---|---|
| Connected device | `GhostStreamForegroundService` | The user taps Start sharing |
| Media projection | `LiveScreenCaptureService` | The user starts Mirror screen and accepts Android's screen-capture prompt |
| Microphone | `LiveScreenCaptureService` | The same Mirror screen session, after the user grants Android's audio permission |

Declare **all three** in the same declaration. Leaving one out is what Play reports as "Permission use
is either not declared or incorrectly declared".

Each type needs a task, a description and a link to a video. Upload the videos to YouTube as
Unlisted (or share them from Google Drive with "anyone with the link") and paste the links:

| Video | Use it for |
|---|---|
| `fgs_connected_device_demo.mp4` | Connected device |
| `fgs_mirror_screen_demo.mp4` | Media projection **and** Microphone (same link for both) |

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

**Task to select:** the option for capturing or recording audio. Do not pick voice calls.

**Description**

```
DirectServe does not record the microphone. During a Mirror screen session the user started, after the user allows Android's audio permission, it captures the audio that other apps are playing (Android's AudioPlaybackCapture API) so the viewer hears it with the mirrored picture. Android requires the RECORD_AUDIO permission and the microphone foreground service type for playback capture. Sound capture starts only inside a mirroring session the user started, and stops when mirroring stops.
```

**Impact if the task is deferred or interrupted**

```
The mirrored picture continues but the viewer hears no sound, or sound drifts out of step with the picture. Audio must be captured in real time alongside the screen, so it cannot be deferred.
```

**Video:** the same `fgs_mirror_screen_demo.mp4`. It shows the audio permission prompt the user
accepts, and a video playing on the phone reaching the laptop while the notification is shown.

---
