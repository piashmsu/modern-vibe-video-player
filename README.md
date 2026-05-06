# Modern Vibe Video Player

A feature-rich Android video player with AI subtitles, AI live captions, smooth
animations, a modern Material 3 UI, and **230+ tunable features** across 10
categories.

> Designed and developed by **Shorif Uddin Piash** — fb.com/piashmsuf

## Highlights

- **AI Subtitles & Captions** — On-device speech recognition (Android
  `SpeechRecognizer`) generates subtitles from spoken audio. A cloud
  Whisper-style hook is also wired in for higher-accuracy transcription
  when the user opts in.
- **Media3 / ExoPlayer** — Hardware-accelerated playback for MP4, MKV, AVI,
  FLV, WebM, TS, HLS, DASH, and RTSP streams.
- **230+ features**, including:
  - Playback: play/pause, seek, loop, shuffle, PiP, autoplay, A-B loop,
    chapter markers, 0.25x → 4x speed presets, pitch correction.
  - Video: HW/SW decoders, HDR passthrough, Dolby Vision, custom aspect
    ratios, brightness/contrast/saturation/hue/gamma filters, sharpen,
    denoise, grayscale, invert, mirror.
  - Audio: 10-band EQ, bass boost, virtualizer, loudness normalization,
    dialog boost, night mode, mono downmix, audio delay, pitch shift,
    AI voice isolate, ReplayGain, passthrough, volume boost +200%.
  - Subtitles: AI generate, AI translate, AI live captions, auto-load,
    OpenSubtitles download, custom font/size/color/outline/shadow/box,
    delay & speed sync, dual subtitles, SRT/ASS/VTT/TTML/PGS support.
  - Gestures: volume / brightness / seek swipe, pinch zoom, pan, two-finger
    rotate, swipe to PiP, long-press for speed, haptic feedback.
  - UI/Theme: dynamic Material You color, glassmorphism, vibe gradient
    backdrops, animated logo, blurred chrome, AMOLED true black,
    parallax headers, scrub thumbnails, cinematic grain, immersive mode.
  - Library: recent history, playlists, favorites, folder/grid/list views,
    fuzzy search, metadata scan, thumbnail cache, Continue Watching,
    USB-OTG, SMB/NAS, DLNA, Chromecast, URL open, YouTube link import.
  - Network: DASH, HLS, RTSP, SMB, FTP/SFTP, custom UA / referer /
    headers, configurable cache & buffer, low-latency HLS, offline cache,
    data saver, ABR auto-quality.
  - AI: on-device & cloud subtitle engines, two-line live captions,
    auto-punctuate, speaker diarization, translation, keyword extraction,
    chapter/full summary, smart chapters, face-detect timeline, silence
    skip, best-thumbnail picker, scene index.
  - Misc: screenshot, record clip, export GIF, share, sleep timer,
    bookmarks, stats overlay, codec info, per-video settings, app PIN
    lock, kids mode, settings export/import, About screen.

Open the in-app **Features** screen to search and toggle every feature
individually.

## Build

Requires JDK 17 and the Android SDK (API 34, build-tools 34.0.0).

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

For a release build:

```bash
./gradlew assembleRelease
```

## Architecture

| Layer | Stack |
|-------|-------|
| UI | Jetpack Compose, Material 3 (dynamic color), Compose Navigation, Compose Animations |
| Playback | AndroidX **Media3 / ExoPlayer 1.2** (`exoplayer`, `dash`, `hls`, `rtsp`, `session`, `ui`) |
| State | SharedPreferences-backed `SettingsRepository` over a `FeatureCatalog` |
| AI | Android `SpeechRecognizer` + cloud Whisper hook (`AiSubtitleEngine`) |
| Service | `MediaSessionService` for PiP / background audio / Bluetooth media keys |

Key files:

- `app/src/main/java/com/piash/modernvibe/videoplayer/MainActivity.kt`
- `app/src/main/java/com/piash/modernvibe/videoplayer/ui/VibeApp.kt`
- `app/src/main/java/com/piash/modernvibe/videoplayer/ui/screens/PlayerScreen.kt`
- `app/src/main/java/com/piash/modernvibe/videoplayer/ui/screens/AboutScreen.kt`
- `app/src/main/java/com/piash/modernvibe/videoplayer/data/FeatureCatalog.kt`
- `app/src/main/java/com/piash/modernvibe/videoplayer/ai/AiSubtitleEngine.kt`

## License

Personal project by Shorif Uddin Piash. All rights reserved unless stated
otherwise in a separate `LICENSE` file.
