# Looking at the screens without a device

The unit tests under `app/src/test/java/dev/nikelauncher/screens/` render the real app on the JVM at
Pixel 9 size (1080 x 2424px at 420dpi, about 411 x 923dp) with Robolectric's native graphics, click through it and save a PNG at each
step to `app/build/screenshots/` (or the folder in the `nike.shots` system property).

Each walk-through needs a test process of its own, because the preferences store outlives a test:

```bash
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.SetupScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.AppScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.HomeScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.ClockConfigScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.NotificationScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.CornerScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.WidgetPagesScreensTest"
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
