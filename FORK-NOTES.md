# HTube — fork notes

## What HTube is

HTube is a modified fork of **QuickTube** by **devgalib**
(https://github.com/devgalib/QuickTube, original commit c835747e14638ba554218aa1d0f49196a7dc6b3c
in this copy). MD did **not** write the original QuickTube and this fork does
not claim otherwise.

- Licence: **GPL-3.0**, unchanged. The `LICENSE` file is intact. If you
  distribute HTube (APK or source), you must distribute it under GPL-3.0,
  keep this note and the original credits, and give recipients the source.
  HTube is **not** closed-source.
- Original credits, kept: devgalib / QuickTube, NewPipe Extractor (GPL-3.0),
  yt-dlp (Unlicense, via youtubedl-android, GPL-3.0), AndroidX (Compose,
  Media3, Room, Navigation, DataStore), Koin, Coil, OkHttp (all Apache-2.0),
  Material Symbols (Apache-2.0), Simple Icons (CC0), Plus Jakarta Sans
  (OFL-1.1). See the original README.md and `docs/`.
- Original warnings, kept: only download content you own or have permission
  to save, respect each site's terms of service, HTube does not bypass DRM,
  and it is not affiliated with YouTube or Google. Google Play and the App
  Store do not allow apps that download YouTube videos, so HTube is **not**
  Play-Store ready and must be sideloaded as a debug/QA APK.
- No Snaptube code, no IDM code, no ads added, no tracking added.

## What changed (and only this)

### 1. Name: QuickTube -> HTube (user-visible only)

- `app/src/main/res/values/strings.xml`: `app_name` = `HTube`
  (launcher label, via `AndroidManifest.xml`), `share_label` =
  `Download with HTube` (share sheet).
- Home hero title, "Do more with HTube", How-To screens, Settings
  feedback/share rows, About screen title, download-sheet browser text,
  update toast, and the default download-folder names
  (`Downloads/HTube`, `Movies/HTube`, `Music/HTube`, `HTube` in
  `core/datastore/SettingsDataSource.kt`, `core/download/DownloadEngine.kt`,
  `feature/settings/SubSettingsScreens.kt`).
- **Not renamed, deliberately:** internal class names
  (`QuickTubeApplication`, `QuickTubeTheme`, `QuickTubeDatabase`), the
  Kotlin package `com.edureminder.quicktube`, and the `applicationId`.
  Renaming the package would touch almost every file and risk breaking the
  build, so it was left alone (see below).

### ApplicationId: BEFORE and AFTER (unchanged)

- Before: `com.edureminder.quicktube` (from `app/build.gradle.kts`)
- After: `com.edureminder.quicktube` — **unchanged**
- Consequence: HTube installs **over** an existing QuickTube install with
  the same signing key, and vice versa. It is not a separate app in the
  Play sense, and the in-app updater still points at the original
  QuickTube GitHub releases (`app/build.gradle.kts`, `UPDATE_API_URL`).
  **Do not accept an in-app update in HTube, or it will replace HTube
  with upstream QuickTube.** If MD later wants a truly separate app, the
  package/applicationId must be renamed in a full refactor and tested
  with a real Gradle build — that was not done here, to avoid shipping
  a broken build.
- Existing downloads: if you already have a `QuickTube` folder on the
  phone, HTube's new default is `HTube`. Move old files into the new
  folder yourself, or pick the old folder in Settings -> Download path.

### 2. Suggestions OFF (the only feature change)

| Section (screenshot) | Status | File(s) | How |
|---|---|---|---|
| Home "Trending now" (Search tab) | **Removed from home** | `feature/home/HomeScreen.kt` (`DiscoverTab`) | The `trending` LazyColumn item and the `TrendingCarousel` composable were deleted. The home tab no longer warms the feed (`LaunchedEffect` page 0 does nothing). |
| Home "Top hits" (Search tab) | **Removed from home** | `feature/home/HomeScreen.kt` (`DiscoverTab`) | The first-music-section row (whose first section is titled "Top hits") was deleted from the home tab. |
| Search live suggestions | **Disabled at source + UI removed** | `feature/search/SearchViewModel.kt`, `feature/search/SearchScreen.kt` | `SearchViewModel.suggestions` is now always an empty list and never calls `media.suggestions()` — typing makes no suggestion network request. The suggestion rows were removed from `SearchScreen`. Typing + pressing Search/Go still works, and a pasted URL still opens the download sheet. |
| Search "Hot searches" chips | **Removed** | `feature/search/SearchScreen.kt`, `feature/search/SearchViewModel.kt` | The "Hot searches" section header and chips were deleted from the UI, and `SearchViewModel.hot` is now an empty list. |
| Search "Recent" (own history) | **Kept** | `feature/search/SearchScreen.kt` | This is the user's own past searches, not a suggestion feed, and it can still be cleared. |
| Search "View sites" shortcuts | **Kept** | `feature/search/SearchScreen.kt` | Static site shortcuts (YouTube/Instagram/Facebook/TikTok/X), not suggestions. |

**Still working, unchanged:** the YouTube tab (page 1, trending feed on
demand when you swipe to it), the Music tab (page 2, music sections on
demand), search results, pasting a link, "Download copied link", share-to-
download, the download sheet, queue, pause/resume, player, browser,
settings, vault, history, and recent downloads. Only the home-tab
previews and the search suggestion surfaces were removed.

### 3. HTube logo (original, not copied)

- `app/src/main/res/drawable/ic_launcher_foreground.xml` — **replaced**:
  a bold white "H" whose centre bar drops into a small download arrow.
  Drawn for HTube only. It is not QuickTube's download glyph, not
  Snaptube's logo, and not YouTube's logo.
- `app/src/main/res/drawable/ic_launcher_monochrome.xml` — **replaced**:
  same H + arrow mark in black, for monochrome launcher themes.
- `app/src/main/res/drawable/ic_launcher_background.xml` — **replaced**:
  deep teal (#0F766E) to indigo (#312E81) gradient, distinct from
  QuickTube's teal-to-blue background.
- `app/src/main/res/mipmap-*/ic_launcher.png` and
  `ic_launcher_round.png` (mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144,
  xxxhdpi 192) — **regenerated** with the same H + arrow design on the
  same gradient (round version clipped to a circle), so older Android
  versions that use the PNGs also show the HTube mark.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and
  `ic_launcher_round.xml` — unchanged, they already point at the
  background/foreground/monochrome drawables above.
- About screen header (`feature/settings/SubSettingsScreens.kt`,
  `AboutScreen`): the generic download glyph in the brand box was
  replaced with a bold "H" mark, so the About header also shows HTube's
  own mark. The home hero icon is still a download icon — that is a
  function icon, not the logo, and was left alone.
- The splash icon (`app/src/main/res/drawable/splash_icon.xml`) reuses
  the launcher background/foreground, so it picks up the new logo
  automatically.

### 4. Smoother / lower latency (safe tweaks only, no feature removed)

- Home tab no longer fires trending + music network calls on launch
  (see section 2) — the home screen shows immediately, with only the
  search box, copied-link chip, quick sites, recent downloads and
  features.
- Search no longer fires a debounced suggestion request on every
  keystroke (see section 2) — typing has no suggestion network latency.
- Thumbnails: `QuickTubeApplication.newImageLoader` now uses
  `.crossfade(false)` instead of `.crossfade(true)`, so list images
  appear immediately instead of fading in. No cache was removed; Coil's
  default memory/disk caching is untouched.
- Kept as-is, because they already help: `HorizontalPager`
  `beyondViewportPageCount = 0` (off-screen tabs are not pre-composed),
  `LazyColumn`/`LazyRow` lists, and background work on `Dispatchers.IO`
  in `QuickTubeApplication` (trash purge, folder import, yt-dlp init).
- **Not done, and not claimed:** no download-engine retuning, no
  threading rewrite, no benchmark was run. "Smoother" here means the
  measurable startup/typing network work above was removed, not a
  measured FPS improvement. No tracking was added to measure anything.

## Build (see README-HTUBE.md for the full steps)

- In Android Studio: open this folder, let Gradle sync, then
  Run > Run 'app' (debug), or Build > Build App Bundle(s) / APK(s).
- From a terminal with the Android SDK: `./gradlew :app:assembleDebug`
  and `./gradlew testDebugUnitTest` (the upstream AGENTS.md gate).
- You need a JDK (17, per the project's Gradle setup) and the Android
  SDK. This VM had neither Java nor an Android SDK, so no APK could be
  built here — see "What was NOT tested" below.

## What was NOT tested (honest list)

- **No APK was built in this VM** (no Java, no Android SDK at the
  start; see the build attempt in the delivery report). No debug APK
  is claimed from this work unless a later build step produced one.
- **No real-phone test:** the app was never installed or run on a
  phone here, so the home screen without Trending/Top hits, the search
  screen without suggestions, the new launcher icon, and the About "H"
  mark have **not** been seen running. MD's first install on his phone
  is the real check.
- **No Gradle compile/test run here** for the same environment reason.
  The edited Kotlin files were checked by inspection only: brace/paren
  balance is 0 in each edited file, the deleted blocks' only callers
  were removed with them, and `grep` confirms no "Trending now",
  "Hot searches", live-suggestion, or user-visible "QuickTube" string
  remains in the edited UI files (internal class/package names and
  code comments excepted, as documented above).
- Play Store submission was not attempted and is not possible for a
  YouTube-download app (upstream README's own store-policy note).

## Files changed in this fork (vs upstream c835747)

```
app/src/main/java/com/edureminder/quicktube/MainActivity.kt
app/src/main/java/com/edureminder/quicktube/QuickTubeApplication.kt
app/src/main/res/drawable/ic_launcher_background.xml
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/drawable/ic_launcher_monochrome.xml
app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png
app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.png
app/src/main/res/values/strings.xml
core/datastore/.../SettingsDataSource.kt
core/download/.../DownloadEngine.kt
core/downloadsheet/.../DownloadSheetHost.kt
core/downloadsheet/.../DownloadSheetViewModel.kt
feature/home/.../HomeScreen.kt
feature/home/.../HowToScreen.kt
feature/search/.../SearchScreen.kt
feature/search/.../SearchViewModel.kt
feature/settings/.../SettingsScreen.kt
feature/settings/.../SubSettingsScreens.kt
FORK-NOTES.md (this file, new)
README-HTUBE.md (new)
```
Everything else is byte-identical to upstream QuickTube at c835747
(except the `.git` directory, which is excluded from the ZIP).
