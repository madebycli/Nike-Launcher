# Nike Launcher handoff

## Branch and rules

Current work: `claude/next`, based on `claude/foundation`. Continue from this branch; starting
again from foundation would discard the changes below. Do not open a PR unless requested.
Read `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/COMPONENTS.md` and `docs/SCREENS.md` first.
Reply to the user in German; repository text is English.

Reference checkouts remain read-only: Victoria Grid `v0.2.0-preview.12` (commit
`f6c370c4aea23a4d208e82edd2debc8c02928a23`) for design/logic, AniSync for reusable components.
Use Material 3 / Material 3 Expressive, then the kit, then the reference components. Keep keys
stable, strings in all six locales, pure logic tested, files below about 400 lines and features
split into modules. No explanatory UI. Do not infer phone performance from screenshots.

Before every push run `testDebugUnitTest lintDebug assembleDebug`. Also build `:app:assemblePerf`
and send its APK as a ZIP at the end of the work block. Do not commit generated output or signing
material. The perf package is `dev.nikelauncher.perf`, release-like and debug-signed.

## Home and preview follow-up

The user's Pixel 9 showed a short favorites list stranded above its resting position, media
visibility moving other blocks, and unstable/low-contrast previews. This follow-up replaces the
per-delta launched scroll snaps with synchronous input and a single Material settling animation.
The short-home viewport cannot acquire phantom scroll space from a misplaced pinned field.
Now Playing retains a fixed field while enabled; editing does not alter frame measurement.
Pins no longer cascade through other pinned fields. The editor uses current references, keeps
pending writes visible through Done, acknowledges encoded precision, and supports grouped selection,
movement, spacing and edge distances. Its extracted Material settings sheet includes favorite-row
spacing. All labels are resources in six locales. HomeLayoutPrefs owns the two new layout keys,
which remain in backup format 2; selected padding edits preserve unselected inherited values.

Clock previews reserve a fixed viewport and choose their Material plate by actual contrast in both
themes. The setup style preview reserves 200dp and scrolls internally; AlignedIconLabel keeps sibling
and right-side icons at the requested dimensions. See ARCHITECTURE.md for module details.

New verification: ordinary HomeScrollInteractionTest, per-module drag/editor/contrast logic tests,
HomeLayoutScreensTest (English/German large text) and PreviewLayoutScreensTest (light/dark).
HomeLayoutScreensTest checks playback/pause/swipe-dismiss bounds, grouped nudging, common padding,
block gaps and stored placement after Done/recreation. HomeScrollInteractionTest records the real
accessibility shade action with a simulated service. Device frame timing and actual system-panel
presentation remain device checks; do not turn those assertions into a smoothness claim.

MediaDismiss wraps the audio card in the stock Material SwipeToDismissBox. A short, interrupted or
cancelled swipe rebounds under Material state ownership; there are no per-event launched snaps.
A new MediaSession receives fresh dismissal state. HomeLayoutScreensTest checks the title's actual
horizontal position after a partial/cancelled swipe before verifying full swipe dismissal.

## Completed in this block

- Rendered all six clock configuration pages, all 20 style previews and the date, number, app,
  calendar and colour pickers. English and German at font scale 1.5, with a real Pixel 9 viewport
  (1080 x 2424px / 420dpi). Added 39 translations each in es/pl/ru/sv; resource coverage is complete.
- Preserved the chosen date format across off/on and saved UI state. Used state-based Material
  text fields for date/number entry; invalid numbers cannot be confirmed. Corrected selected
  style contrast and the weather app's empty selection label. Split calendar picking from styles.
- Added paint-only home gesture zones: configured edges/band/insets/corner keep-out, quick-launch
  region and app labels, pull-down and swipe-up. Labels are resources in all six locales. The
  overlay sits below the nudge panel and edit toolbar. Filled Material chips keep frame labels
  legible over wallpaper. Existing frame/content nudge behaviour is retained.
- Rendered notification overview, conversation, reply, inline attachment, big picture and plain
  detail. Added grouped authors, newest-message anchoring, Material swipe hiding/dismissal,
  metadata-based provider actions, proper large-picture resolution and stable message keys.
  Reply drafts retain their target across provider updates; send waits for explicit retargeting.
- Added setup defaults for clock/calendar/weather apps, included in the existing backup. New
  clocks receive them; reopening a saved clock retains its own settings. Reused the clock app
  pickers. Split Access/Favorites steps; use the existing reorderable library and up/down buttons.
  Removed an extra setup-wide clickable modifier that consumed the reorder handle's down event.
  The setup render test verifies the stored order after both an arrow move and a drag.
- Added floating-search foreground/automatic-foreground controls using the already existing
  persisted keys. Pinned the corner-button preview above its scrolling controls. Rendered the
  actual side-page grid and its resize dialog; extracted its toolbar into a component using
  stock Material button shapes. Removed its old file-size exception from `ProjectRulesTest`.
- Added a real backup round-trip test, including a newly allocated clock id, all relevant clock
  options, new defaults, horizontal offsets, quick-launch region and search colours.
- Measured band-state invalidations before isolating the read in `ObserveScrubBand`: the focused
  120-update probe changes outer compositions from 121 to 1; visible consumers still update and
  hidden consumers do not subscribe. This is not a device frame-time benchmark.
- Rendered actual German weather sheet headers at font scale 1.5. They fit the Pixel 9 viewport.
  Updated architecture and comparison/performance records.

## Verification entry points

Run screenshot classes one at a time, each in its own test process. `NIKE_SCREENS=1` enables
ClockConfig, Home, Notification, Setup, Corner, WidgetPages and App screen tests. Weather tests
run without the flag. See `docs/SCREENS.md` for exact commands and limitations.

`ScreenshotRule` uses native Canvas software PixelCopy. Robolectric's embedded hardware renderer
could omit unchanged RenderNodes after a previous capture; do not treat that capture artifact as
an app bug. Dialog coordinates are simulated rather than a phone window-compositor check.

`NIKE_PERF=1` enables `BandRecompositionTest`. The test keeps the original read-pattern probe as
well as the production isolation wrapper and hidden-consumer probe.

The real backup test is `data.backup.SettingsBackupRoundTripTest`. Its fake AppWidgetHost must
allocate a positive id (101); Robolectric's default 0 is an invalid widget id.

If lint's Kotlin analysis worker reports a disposed symbol pointer, rerun in a fresh process
with `--no-daemon --no-parallel --max-workers=1`; do not disable lint detectors. Avoid editing
sources while a check is reading them. A cold perf build can spend several minutes in R8.

## Remaining device work

- Install the delivered perf APK on the user's Pixel 9 and assess scrolling, A-Z scrubbing,
  app-list opening and animations. Record frame times or a Macrobenchmark before further tuning.
- Record and validate a Baseline Profile on a device. No profile was fabricated in the cloud.
- Confirm live notification/reply/provider behaviour, Android home-role/widget binding dialogs,
  installed icon packs and real wallpaper/Material You colours. Cloud fixtures do not verify them.
- Shared settings are still collected in `NikeNavHost`; no per-frame emission was measured there,
  so this block did not rewrite global state speculatively.
- Double-tap-to-lock uses the app-list/edge handler; bare home wallpaper has no double-tap handler.

`docs/GRID-COMPARISON.md` records the reference audit and the boundary between source review,
rendered layout and device verification. `docs/PERFORMANCE.md` records the measurement method.
