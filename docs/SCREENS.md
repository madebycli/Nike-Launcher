# Looking at the screens without a device

The unit tests under `app/src/test/java/dev/nikelauncher/screens/` render the real app on the JVM at
Pixel 9 size (1080 x 2424px at 420dpi, about 411 x 923dp) with Robolectric's native graphics, click through it and save a PNG at each
step to `app/build/screenshots/` (or the folder in the `nike.shots` system property).

Each walk-through needs a test process of its own, because the preferences store outlives a test:

```bash
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.SetupScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.UserFlowScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.MenuMotionScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.AppScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.HomeScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.HomeLayoutScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.MediaWidgetScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.PreviewLayoutScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.ClockConfigScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.NotificationScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.CornerScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.WidgetPagesScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.RoundedWidgetScreensTest"
./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.WeatherScreensTest"
```

Without `NIKE_SCREENS` they are skipped, so a plain `testDebugUnitTest` stays fast.

What this shows: layout, text fit, colours, spacing, state after a click. What it does not: animation
timing, gestures with real velocity, wallpaper, widgets from other apps, or anything about speed.
Fake apps come from `FakeApps.kt`; the colours are the fallback theme, not a phone's Material You palette.

`ClockConfigScreensTest` visits all six clock settings pages, scrolls through every style and
appearance section, and opens date and exact-number dialogs. It also runs in German at font scale
1.5, checks invalid number entry, and verifies that toggling the date and restoring saved UI state retain the selected format.
Its widget id is local to the test; saving is captured in memory without installing a widget.

`Pixel9Display` also fixes the physical window size; qualifiers alone round the dp viewport down
by a couple of pixels. `ScreenshotRule` uses native Canvas software PixelCopy: reusing Robolectric's
embedded hardware renderer could omit unchanged RenderNodes after the first capture. This affects
capture reliability, not the app's rendering configuration.

Notification fixtures include a conversation, inline attachment, large picture, plain notification,
reply suggestions and action replacement while a draft is open. Corner settings are rendered in
German at font scale 1.5. The real widget-page grid exercises its size dialog. Weather tests render
the actual sheet and its German overview/daily/hourly headers with invented provider data.

The focused recomposition probe is separate from screen captures:

```bash
NIKE_PERF=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.BandRecompositionTest"
```

Its counters measure snapshot invalidation scopes, not GPU performance or frame time on a device.

Dialog window coordinates are simulated by Robolectric. Dialog content can be checked in its
local window, but captures do not establish Android's final on-device window positioning.

`HomeLayoutScreensTest` uses the real home screen, a local MediaSession and real preferences.
It asserts identical favorite/frame bounds while playback starts, pauses and is dismissed by a
horizontal swipe. A partial swipe interrupted by a cancelled drag must return the audio title to
its original horizontal coordinate before full dismissal is tested. It also verifies rebound on the real home, unchanged bounds on entering editing,
group nudging, common padding, block spacing, Done and a newly created editor. English and German
at 150% font scale produce `home-fixed*` images. Data and app-launch boundaries are fixtures.

`PreviewLayoutScreensTest` renders the real first-run StyleStep and clock PinnedPreview in light
and dark themes. It checks exact preview bounds while changing icon size, spacing, label size and
clock size, and checks both icons retain their full requested dimensions, including icons to
the right of large labels. `preview-fixed*` images include white and black clocks.

`HomeScrollInteractionTest` runs in ordinary unit-test checks. It drives the real draggable and
animation clock through release, cancellation, an interrupted rebound and swipe-up completion.
Its deep-pull test records GLOBAL_ACTION_NOTIFICATIONS through the real SystemUi/accessibility
service path with a Robolectric service. This verifies the request, not the on-device system panel.

The follow-up setup test recreates the real activity on Access and checks that Welcome does not
return. Selected and available favorite rows share height and Material styling; the real drag is
checked against the stored order. Preview tests assert exactly two full-size icons and that the
preview scrolls away with its controls.

`UserFlowScreensTest` navigates from Home settings with the corner disabled, restores it, drags its
separate placement draft, resets and saves it. It then injects in-memory notifications and swipes
on home (with a widget page enabled) and the app list, checking that the app overview opens and no
notification cancellation occurs. It also exercises corner search and the independent field edges.
`MenuMotionScreensTest` renders the real widget menu and samples intermediate measured positions
for the shared settings transition and both search-field origins using the production Material
specs. These checks prove direction/state under the test clock, not smoothness on a phone.
