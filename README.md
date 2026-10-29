# HTube v1 — read this first

HTube is a modified fork of QuickTube (devgalib, GPL-3.0). MD did not write
the original QuickTube. See `FORK-NOTES.md` for the full fork note, credits
and licence rules. **This file is only the short version.**

## What changed

- App name is **HTube** everywhere you see it: launcher, share sheet
  ("Download with HTube"), home title, About, settings, download sheet.
- **Suggestions are OFF:** no "Trending now" and no "Top hits" on the home
  Search tab, no live search suggestions, no "Hot searches" chips.
  Search itself, pasting a link, "Download copied link", share-to-download,
  download, queue, pause/resume, player, browser, settings, vault and
  history all still work. The YouTube and Music tabs still show their feeds
  when you swipe to them — only the home-tab previews were removed.
- **New logo (2026-10-29, MD's own image):** a gold metallic "H"
  with a blue swoosh arrow, on a dark background. The full banner
  (gold H + "HTube" text) is in `drawable/htube_logo_banner.jpg`.
  Not Snaptube's or YouTube's logo.
- **A bit smoother:** home no longer loads trending/music on launch,
  search no longer calls the suggestion service while typing, and
  thumbnails no longer crossfade. No features were removed to gain speed
  and no tracking was added.

## What stayed the same

Everything else: download engine, formats (144p–4K, M4A/Opus), playlists,
batch download, browser, player, vault, recycle bin, settings, and all
credits. ApplicationId is now `com.mdhasan.htube`, and the in-app
updater points at the HTube repo (hasan899420/HTube) — HTube is a
separate app and is no longer updated from any other app's releases.
See FORK-NOTES.md for the full fork note.

## How to build (Android Studio, on your PC)

1. Open this folder in Android Studio and let Gradle sync (first sync
   downloads dependencies; it takes a while).
2. Connect your phone with USB debugging, or start an emulator.
3. Run > Run 'app' — this installs the debug build on the phone.
4. Or, for an APK file: Build > Build App Bundle(s) / APK(s) > Build
   APK(s), then find it under `app/build/outputs/apk/debug/`.
5. From a terminal (Android SDK + JDK 17 installed):
   `./gradlew :app:assembleDebug` and `./gradlew testDebugUnitTest`.

## Install on your phone

1. Copy the debug APK to your phone and open it.
2. Allow "Install unknown apps" for the app that opens it, when asked.
3. HTube is a debug build, not a release build and not Play-Store ready.
   Google Play does not allow YouTube-download apps.

## What was NOT tested

- No APK was built in the Linux VM used to make this fork (no Java and
  no Android SDK there), and the app was never run on a real phone there.
- Your first install on your phone is the real check: home without
  Trending/Top hits, search without suggestions, the new HTube icon,
  and a normal download (search, paste a link, share-to-download,
  pause/resume) should all be checked there.
- If a build fails in Android Studio, send the exact Gradle error text —
  do not guess, and do not install an APK from a random site instead.
