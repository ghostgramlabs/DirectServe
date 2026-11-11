# Foreground service declaration

Play Console → Policy and programs → App content → Foreground service permissions.

Answer **Yes**, the app uses foreground service permissions, and declare these three types.
They match `app/src/main/AndroidManifest.xml`; the merged release manifest adds no others.

| Type | Service | Started when |
|---|---|---|
| Connected device | `GhostStreamForegroundService` | The user taps Start sharing |
| Media projection | `LiveScreenCaptureService` | The user starts Mirror screen and accepts Android's screen-capture prompt |
| Microphone | `LiveScreenCaptureService` | Same as above, only when the user allows sound |

Each type needs a description and a link to a short video that shows the feature (an unlisted
YouTube link or a shared Google Drive link works).

---

## Connected device (`FOREGROUND_SERVICE_CONNECTED_DEVICE`)

**Description**

```
DirectServe turns the phone into a local media server so other devices on the same Wi-Fi or hotspot (a TV, laptop, tablet or phone) can browse, stream and download the files the user chose to share. The foreground service starts only when the user taps "Start sharing" and keeps the local web server reachable for the connected devices while the user switches apps or the screen turns off. A persistent notification shows that sharing is on, how many devices are connected, and a Stop button. The service stops when the user taps Stop.
```

**Impact if the task is deferred or interrupted**

```
Connected devices immediately lose access: videos stop mid-playback, downloads and uploads fail, and the TV or laptop shows the share as unavailable. The task cannot be deferred because the other device is actively requesting files in real time.
```

**Video should show:** choosing files → Start sharing → the notification → opening the address on
another device → playing a video there while the phone is on another app → tapping Stop.

---

## Media projection (`FOREGROUND_SERVICE_MEDIA_PROJECTION`)

**Description**

```
DirectServe's "Mirror screen" feature shows the phone's screen live in a web browser on another device on the same local network, for presentations, demos and showing photos to a room. It starts only when the user opens Mirror screen, taps Start and accepts Android's screen-capture prompt. The foreground service holds the MediaProjection session and streams the captured screen to the viewer's browser over the local network. Android's capture indicator and a DirectServe notification with a Stop button are shown for the whole session.
```

**Impact if the task is deferred or interrupted**

```
The live view on the other device freezes or goes black and the presentation stops. Mirroring must run continuously while the user uses other apps, so it cannot be deferred.
```

**Video should show:** Mirror screen → Start → Android's "Start recording or casting?" prompt →
the phone screen appearing live in a laptop browser → switching apps on the phone → Stop.

---

## Microphone (`FOREGROUND_SERVICE_MICROPHONE`)

**Description**

```
DirectServe does not record the microphone. During Mirror screen, when the user chooses to include sound, it captures the audio that other apps are playing (Android's AudioPlaybackCapture API) so the viewer hears it with the mirrored picture. Android requires the RECORD_AUDIO permission and the microphone foreground service type for playback capture. Sound capture starts only inside a mirroring session the user started, and stops when mirroring stops.
```

**Impact if the task is deferred or interrupted**

```
The mirrored picture continues but the viewer hears no sound, or sound drifts out of step with the picture. Audio must be captured in real time alongside the screen, so it cannot be deferred.
```

**Video should show:** the same mirroring session with a video playing on the phone and its sound
heard on the laptop.

---

One video can cover all three: start sharing, play a video on the laptop, then start Mirror screen
with sound, and stop both. Use the same link for each type.
