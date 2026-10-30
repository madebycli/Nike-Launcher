# Handoff prompt for the next AI (Nike Launcher)

Copy everything below the line into the next session. Work on a NEW branch cut from `claude/foundation`
(for example `claude/next`), commit small, push, and do not open a PR unless asked.

---

You are continuing **Nike Launcher** (`madebycli/nike-launcher`, package `dev.nikelauncher`, Android, Kotlin,
Jetpack Compose, Material 3 Expressive). Base: branch `claude/foundation`. Reference apps, read-only, never modify:
Victoria Grid tag `v0.2.0-preview.12` (`madebycli/victoriagrid`) is the design and logic reference the user wants
matched ("the last preview"); AniSync (`Marco-9456/AniSync`, GPL-3.0) is the source of UI components.
Reply to the user in German, write everything inside the repo in English.

## The rules (AGENTS.md is binding; read it first)

1. **Use the prebuilt component, never rebuild one.** Material 3 / M3 Expressive, then `ui/kit/`, then AniSync. Rows are
   `ui/kit/settings` (`SettingsGroup`, `settingsItem`, `settingsSwitch`, `settingsBlock`, `ChoiceChips`, `SettingsSlider`,
   `ColorPickerSheet`, `SettingsScaffold`, icon plates via `IconBlob`). Shapes from `MaterialTheme.shapes`, motion from
   `ui/kit/motion/AppMotion`, colours from `MaterialTheme.colorScheme`. No `RoundedCornerShape(`, no hand-drawn pills, no
   look-alikes. If nothing fits, write it in `ui/kit/` with a one-line comment saying what was searched.
2. **Modules, no monoliths.** One concept per file, one feature per folder (a screen is a folder: `FooScreen.kt` wiring,
   `FooState.kt`, `FooViewModel.kt`, `components/`). Files stay under about 400 lines, past 600 they must be split first.
   `ProjectRulesTest` enforces this (legacy allow-list: `AppListScreen` 1868, `AppRepository` 745, `WidgetGridPage` 450 —
   never grow them, shrink them when you touch them). Pure logic lives apart from Compose and gets a unit test.
   Moving a button or renaming a setting must touch one file. Update `docs/ARCHITECTURE.md` in the same commit.
3. **Text is data.** No user-visible string literal in Kotlin. Strings go in `res/values/strings.xml` (English) and
   `values-de/es/pl/ru/sv`. Labels must survive +40 % length and font scale 1.5. Never rename a string key.
4. **English everywhere in the repo.** Do not mention projects the app was derived from except Victoria Grid and the
   AniSync attribution header (`// Adapted from AniSync (GPL-3.0-or-later), <path>` + `NOTICE.md`).
5. **The UI does not explain itself.** No hint banners, no tutorial text, no apologies. An icon or one word is enough.
6. **Honest verification.** Say what you ran and what you saw. Never claim a gesture or animation works from a still.
7. Settings keys are persisted data: never rename or retype one without a migration. Every preference is included in
   the backup automatically (`Prefs.storedValues`), so a new key needs no extra wiring but needs a restore check.
8. Small commits, imperative subject. Before pushing: `./gradlew testDebugUnitTest lintDebug assembleDebug`. Never push a
   red build. Never commit signing material.

Build: `export ANDROID_HOME=/opt/android-sdk JAVA_TOOL_OPTIONS=` then Gradle. A release-like test APK for the user:
`./gradlew :app:assemblePerf` (signed with the debug key, id `dev.nikelauncher.perf`). **Send the user a test APK at the end
of every work block** (zip it; the client renames `.apk`).

## How to look at the UI (the emulator is far too slow; do not use it)

`docs/SCREENS.md`: Robolectric renders the real app on the JVM at Pixel 9 size and saves PNGs to
`app/build/screenshots/`. Run one class at a time:
`NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.SetupScreensTest"` (also `AppScreensTest`,
`HomeScreensTest`; `WeatherScreensTest` needs no flag). Fake apps come from `FakeApps.kt`. Open the PNGs and LOOK at
every screen you touch. Colours there are the fallback theme, not the user's Material You palette; animations, lag and
real gestures cannot be judged there.

## State right now

Done and pushed on `claude/foundation`: setup flow with a Style step (live preview), icon plates on all settings rows,
animated search pill, favorites chooser without checkboxes, edit-app sheet (name and icon), compact weather sheet header,
Google Sans Flex / Niagara fonts, `perf` build type, `HomeScroll` made stable (it was recreated with a zero viewport,
which made the home jump to the top and edit mode snap back), A-Z band and list controller no longer read stale values
(the A-Z band set by hand was saved but never shown), default clock retried on resume, reset-clock-colour row,
JVM screenshot tests, arrow nudging for home blocks.

Home edit mode, as the user wants it (finished in the last commit): the **field (block frame) only moves up and down**
with up/down arrows (1 dp a press, bigger steps while held, current top shown in dp) and stays against both screen
edges. The **content inside the field** can be moved left/right with arrows (`BlockOffsets`, stored per block in
`block_offset_*`, drawn inside the frame and clipped to it) and its **distance from the edge** has − / + (rows use
`sidePaddingDp`, the widget uses `widgetSidePaddingDp`). All in `ui/home/edit/NudgePanel.kt`; verify with `HomeScreensTest`.

## What is left, in priority order

1. **Clock widget settings in the Victoria Grid design (the user rates Grid's version 10/10, current Nike one is flat).**
   The new screen already exists in `widget/config/` (`ClockConfigScreen.kt` pages HOME / CLOCK / STYLES / CALENDAR /
   WEATHER / APPEARANCE, `PinnedPreview.kt`, `ClockPages.kt`, `ClockPagesStyle.kt`, `ConfigNumber.kt`) and compiles, but it
   has **never been rendered**. Add a screenshot test for it (it takes `widgetId`, `initial`, `onSave`, `onCancel`;
   `rememberClockLiveState` needs a real widget id or an override), look at every page, compare with Grid's
   `widget/ClockWidgetSettings.kt`, `ClockStylePicker.kt`, `ui/settings/PreferenceRows.kt` in `v0.2.0-preview.12`, and fix:
   pinned preview height, row summaries, icons, style gallery, exact number entry, dialogs. Missing translations for the
   new keys in es/pl/ru/sv (run `/tmp`-style string porting from Grid res where Grid has them, translate the rest by hand).
   Delete nothing the user used: the old row builders were removed, so check every option from the old screen still
   exists on some page (hour format, date format incl. custom, battery, colour, weather unit/app, agenda count/lookahead
   and relative labels, calendars, clock/calendar app, text width, gap, size/opacity per part).
2. **Show where the swipe gestures are.** Edit mode on the home screen should draw the gesture zones as labelled,
   touch-transparent overlays: A-Z edge zone (`edgeSide`, `edgeZoneWidthDp`, band), quick-launch region
   (`QuickLaunchRegion`, left/right apps), pull-down (notifications) and swipe-up (app list). Settings has a
   flash-preview for the edge zone already (`SettingsPreviews`); reuse its ideas. Strings in all six locales.
3. **Messages UI.** Render and fix the notification sheet (`ui/notifications/*`: overview, conversation, reply, images)
   with fake data in a screenshot test; compare with Grid `NotificationSheet/Overview/Conversation/Parts`.
4. **Finish the Victoria Grid comparison item by item** (the user's real goal: every Grid feature present and verified).
   Compare string keys and screens of `v0.2.0-preview.12` against Nike; known gaps to check: corner button settings and
   preview, search-button colours, widget pages editing, backup/restore round trip with the new keys, text-width and
   spacing options, icon pack choice in setup, default clock/calendar/weather apps in setup (Grid had them), Setup
   "Welcome" parity with Grid's `WelcomeDialog` (the user said it felt empty — the Style step is the answer; check the
   Favorites step has Grid's drag-to-reorder and up/down buttons).
5. **Performance.** The user reports the perf build as laggy and the home as janky. Suspects, unmeasured: the A-Z band
   state written every frame while the home is dragged (`HomeScreen` favorites `onGloballyPositioned` →
   `BandEditing.favoritesBand`), the whole-app settings state collected once in `NikeNavHost`, no baseline profile.
   Measure before changing (Macrobenchmark or `adb shell dumpsys gfxinfo` on a real device is not available here;
   at least count recompositions with the Compose tracing or reasoning from state reads).
6. **Remaining small items:** weather sheet "title width" complaint (header was compacted, never rendered with the
   user's German text), `docs/ARCHITECTURE.md` entries for `widget/config/`, `home/edit/NudgePanel`, `BlockOffsets`.

## Process notes

- The user communicates in German, is impatient, and interrupts long silent commands: keep commands short, run builds in
  the background, tell them what you are doing.
- Do not tear the app apart for its own sake; split a monolith only when a feature touches it.
- Be honest about what was seen. The user checks on a real phone and sends screenshots/videos; read them carefully
  (`ffmpeg` is installed for video frames).
