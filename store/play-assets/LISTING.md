# Play Store listing: DirectServe 1.0.27

Ready to paste into Play Console → Grow users → Store presence → Main store listing.
Each heading gives the Play Console field and its character limit; every block below fits.

---

## App name (max 30)

```
DirectServe: Cast & File Share
```

## Short description (max 80)

```
Play phone videos on your TV or laptop, mirror your screen and share files.
```

## Full description (max 4000)

```
Watch your phone's videos, photos and music on a TV, laptop or tablet, and move files between devices over your own Wi-Fi. No cables, no account, no uploading. The other screen only needs a web browser.

HOW IT WORKS
1. Choose what to share: videos, photos, music or files.
2. Tap Start sharing.
3. Scan the QR code, or type the short address, on the other device.
That's all. Nothing to install on the TV or computer.

YOUR OWN STREAMING PAGE
The browser opens a home page that feels like a streaming app: a featured video with a big Play button, rows to swipe through for each folder, photos and music, and a Continue watching row that picks up where you stopped.

PLAYS STRAIGHT FROM YOUR PHONE
• Videos play directly from your phone, with no copying first.
• If a browser can't play a file as it is, DirectServe prepares a compatible version when you press Play.
• In full screen, wide videos turn the phone sideways by themselves.
• Photos open full size; music plays right in the page.

SHARE FILES BOTH WAYS
• Download one file, a selection, or everything as a ZIP.
• Anyone in the session can send photos, videos and files to your phone from their browser. You approve each transfer.
• Add files or whole folders at any time. Open pages update by themselves.

MIRROR YOUR SCREEN
Show your phone's screen live in a browser, with sound from apps that allow it. Great for presentations, demos and sharing photos with a room.

SEND TEXT AND LINKS
Paste a link or a note on the laptop and it appears on your phone, or the other way round.

TVs AND MEDIA PLAYERS
Turn on DLNA in Advanced settings so smart TVs, VLC and Kodi on the same network can find your phone.

PRIVATE BY DESIGN
• Works only on your Wi-Fi or your phone's hotspot. Your files never leave your local network.
• No account, no cloud, no ads.
• Add a PIN so only people you tell can open your share.
• See who is connected, rename or block devices, and stop sharing with one tap.

DirectServe is open source (Apache 2.0) and available in 23 languages.

Good to know: speed depends on your Wi-Fi or hotspot. Some apps block screen and sound capture.
```

## What's new in 1.0.27 (max 500)

```
• A simpler app: choose what to share, tap Start, scan the code.
• Browser home like a streaming app, with Continue watching.
• Add files in one step; the browser updates by itself.
• Wide videos turn the phone sideways in full screen.
• New app icon.
• Mirror screen sound now stays in step with the picture.
• Long sharing sessions no longer stop after a few hours.
• Crash fixes for a busy port and Live Screen refresh.
• Lower memory use with large photos, and a smaller download.
```

---

## Graphics (upload in this order)

| File | Play Console field | Caption used in the image |
|---|---|---|
| `icon_512.png` | App icon (512 × 512) | |
| `feature_graphic_1024x500.png` | Feature graphic | |
| `phone_01_home.png` | Phone screenshot 1 | Share in 2 taps |
| `phone_02_session.png` | Phone screenshot 2 | Scan and open in any browser |
| `phone_03_laptop.png` | Phone screenshot 3 | Open it on any screen |
| `phone_04_browser.png` | Phone screenshot 4 | Your own streaming page |
| `phone_05_player.png` | Phone screenshot 5 | Plays straight from your phone |
| `phone_06_mirror.png` | Phone screenshot 6 | Mirror your screen live |
| `phone_07_library.png` | Phone screenshot 7 | Always know what you're sharing |
| `phone_08_dark.png` | Phone screenshot 8 | Private and offline |

The address shown in the screenshots (`192.168.1.8:43183`) is an example home-network address;
43183 is the app's real default port.

## Store settings

| Field | Value |
|---|---|
| App category | Video Players & Editors (alternative: Tools) |
| Tags (pick up to 5) | Video players, Media players, File transfer, Screen mirroring, Media server |
| Contains ads | No |
| In-app purchases | No |

---

## Notes for editors

- Claims follow the Do-Not-Claim list in `store/PLAYSTORE_LISTING.md`: no cloud, no internet access,
  no "instant playback for every file", no "most secure", no unlimited speed.
- The title and short description carry the most search weight, so they use what people search
  for: "cast", "TV", "mirror screen", "file share". "File transfer" alone competes head-on with
  Xender, ShareMe and Send Anywhere; "share to browser" has almost no searches.
- "23 languages" counts distinct languages; regional variants (es-US, pt-BR, zh-TW) are not
  counted separately.
- Check the lengths after editing: `python -I store/play-assets/check_listing.py`.
