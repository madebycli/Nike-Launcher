# Looking at the screens without a device

The unit tests under `app/src/test/java/dev/nikelauncher/screens/` render the real app on the JVM at
Pixel 9 size (411 x 924 dp) with Robolectric's native graphics, click through it and save a PNG at each
step to `app/build/screenshots/` (or the folder in the `nike.shots` system property).

Each walk-through needs a test process of its own, because the preferences store outlives a test:

```bash
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.SetupScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.AppScreensTest"
NIKE_SCREENS=1 ./gradlew :app:testDebugUnitTest --tests "dev.nikelauncher.screens.HomeScreensTest"
```

Without `NIKE_SCREENS` they are skipped, so a plain `testDebugUnitTest` stays fast.

What this shows: layout, text fit, colours, spacing, state after a click. What it does not: animation
timing, gestures with real velocity, wallpaper, widgets from other apps, or anything about speed.
Fake apps come from `FakeApps.kt`; the colours are the fallback theme, not a phone's Material You palette.
