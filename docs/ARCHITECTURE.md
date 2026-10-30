# Architecture

Nike Launcher is a single-module Android home-screen launcher: Kotlin, Jetpack
Compose, `minSdk 26`, `compileSdk`/`targetSdk 37`. No network permission, no
analytics, no ads, no accounts.

## Package layout (`dev.nikelauncher`)

```
MainActivity.kt                 Activity, window setup, status-bar peek
NikeApp.kt                  Application; owns Prefs, repositories, widget host
data/
  AppInfo.kt, AppRepository.kt  component + label, PackageManager queries, launch, app-info intents
  IconPackRepository.kt         icon-pack discovery, appfilter.xml parsing, icon lookup
  Prefs.kt                      owns the DataStore; settings live in Library/Appearance/Home/AppList/Strip/Corner/WidgetPagePrefs.kt, keys in PrefKeys.kt
  backup/                       the backup file: typed values, widgets with new ids, files the settings point at, widgets left to confirm
  Folder.kt                     folder model + JSON persistence + favorites token encoding
  HomePins.kt                   where each home block is held (edge + fraction)
  WidgetPages.kt                the grids of widgets on the pages either side of home
  CornerButton.kt               the corner button's actions, shapes and colors
ui/
  NikeNavHost.kt                collects settings once, hosts the navigation graph, shows the first-run setup
  home/HomeRoute.kt             home destination: pages, app-list overlay, edge zones
  home/route/                   the pieces of the route: list host, wallpaper layers, page host, gesture exclusions
  home/stack/, blocks/, rows/   the pinned stack, its blocks, favorite and folder rows
  home/edit/                    edit mode: frames, drag handles, snap guides
  home/pages/                   widget pages: pager, grid, resize and reorder
  home/corner/                  the corner button, its sheets and the floating search button
  home/setup/                   the first-run flow; StyleStep pins a StylePreview (the favorites, read from the same prefs) above the style controls
  home/wallpaper/               system blur, picture and grain behind the app list
  applist/                      A-Z list, pull-to-collapse, strip and edge zone, scrub state
  notifications/                the notification sheet: overview, conversation, actions, reply
  settings/                     settings screen, its sections and the picker screens
  theme/                        Material 3 Expressive theme, fonts, wallpaper-aware text color
  kit/                          shared building blocks: motion, menu, settings rows, onboarding (see docs/COMPONENTS.md)
media/                          notification listener, preview bus and parser, Now Playing
service/                        haptics, status-bar fader, accessibility service, system UI
widget/                         clock widget (model, faces, weather, calendar, overview sheets), widget host, picker, restore
```

## Things that look wrong until you know why

**The launcher does not draw the wallpaper.** `Theme.NikeLauncher` sets
`android:windowShowWallpaper`, so the real system wallpaper shows through a
transparent window. "Dim wallpaper" is therefore just a scrim `Box`, not image
processing.

**`LongPressFrameLayout` wraps every embedded widget** instead of a Compose
`pointerInput`. A Compose long-press detector owns the whole touch stream once it
starts tracking, which would break the widget's own buttons. This mirrors how
scrollable containers arbitrate with children: don't intercept on `ACTION_DOWN`,
watch via `onInterceptTouchEvent`, steal the stream only once our timer fires.

**The A-Z strip bows; the letters never scale.** Scaling the glyphs stretched them
until they looked pixelated, so `EdgeScrubber` translates each letter along a
gaussian instead. `scrubY`/`pullPx` arrive as lambdas read inside `graphicsLayer`,
which puts the read in the draw phase — recomposing 26 `Text` nodes per pointer
move made this jitter.

**`recordTouchPosition` deliberately does not consume the down event.** It exists so
a row can place its long-press menu at the finger while `combinedClickable` and any
scrolling parent still arbitrate normally. Using `detectTapGestures` instead
consumes the down event and silently stops the parent from ever seeing the drag.

**The app-list overlay stays composed while hidden.** It is measured but never
placed (`layout(0, 0) {}`), so it neither draws nor receives touches. Building the
list from scratch on every open is what made it take a beat to appear. The home
screen behind it is likewise kept laid out — an `AppWidgetHostView` that is never
placed loses its layout and returns with its text collapsed.

**Gesture exclusion is spent on the scrub band, not the whole edge.** Android
honors only 200dp of exclusion per side, so `HomeRoute` clips the excluded
rectangle to the band the strip actually occupies.

**Notification content stays in memory.** The preview bus holds what the sheet shows only while
the phone is unlocked and previews are on; locking, switching it off or losing the listener
clears it. Replies and actions go through the bus, which checks each request against the
notification as it is now.

**A backup carries more than settings.** Widgets get new ids on another phone, so each is bound
again and every reference (home, folders, widget pages) is remapped; a widget the system will not
bind by itself waits in `WidgetRestoreActivity`. Files the settings point at (a font, icons, the
list picture) travel inside the file, checked before anything is written.

## Recomposition

Two deliberate choices keep scrubbing off the main thread's back:

- **`ScrubState`** holds the letter, position and elastic pull. It is passed down as
  one stable object so only the composable that *reads* a field is invalidated. When
  the home destination read the letter directly, each of the ~26 letter changes per
  gesture invalidated that whole scope — and the home screen hangs off it.
- **Icons are rasterised once into a byte-bounded `LruCache`** and pre-warmed off the
  main thread, favorites first. The cache is bounded by bytes rather than entry
  count because the cache key includes the rasterised pixel size: moving the
  icon-size slider adds a generation rather than replacing one, and a single 96dp
  icon on a dense screen is ~450 KB.

## Permissions

The app requests **no `INTERNET` permission**. It cannot phone home.

| Permission | Why | Optional? |
|---|---|---|
| `QUERY_ALL_PACKAGES` | A launcher must enumerate every installed app, icon pack and widget provider. Without it `PackageManager` filters almost everything out on Android 11+. | No |
| `EXPAND_STATUS_BAR` | Needed by the pre-Android-12 reflection fallback for opening the notification shade. | No |
| `VIBRATE` | The haptic tick as the finger crosses a letter. Respects the system touch-feedback setting, and can be turned off in Settings. | No |
| Accessibility service | The only sanctioned way to open the notification shade or lock the screen — neither has a public API. The service ignores every event it receives and reads nothing. | Yes — the swipe-down and double-tap-to-lock gestures simply report that they need it |
| Notification listener | Reads the active media session for the Now Playing card. | Yes — off by default |

`BIND_APPWIDGET` is **not** requested: it is signature-level and is never granted to
an ordinary app. `AppWidgetHost` and `bindAppWidgetIdIfAllowed` work without it for
whichever app is the current Home.

## Known gaps

- No search in the A-Z list; the edge scrubber is the only way to find an app.
- Work-profile apps are invisible. `AppRepository` uses `PackageManager` rather than
  `LauncherApps`, and `AppInfo` carries no `UserHandle`. Fixing this changes
  `AppInfo.key`, which is the persisted favorites key, so it needs a data migration.
- Double-tap-to-lock works only in the app list, not on the home screen.
- No Baseline Profile. Generating one needs a device or emulator; it is the largest
  remaining first-launch win.
- Unit tests cover only framework-free logic. Anything touching `PackageManager`,
  `ComponentName` or Compose needs Robolectric or an instrumented test.

## Clock configuration

Clock configuration uses a saved draft until Done, including the previous enabled date format.
Date and exact-number entry use state-based Material fields. Calendar selection lives apart from
the style gallery; selected previews use the matching content colour. ClockAppLabel is shared.
ClockConfigScreensTest renders six pages, all twenty styles and dialogs at Pixel 9 dimensions,
in English and German at 150% font scale. Missing clock strings are supplied in es/pl/ru/sv.
Native Canvas software PixelCopy avoids missing unchanged RenderNodes in repeated captures.

## Home edit annotations

HomeGestureZones draws Canvas rectangles and stock Material badges without pointer handlers,
below the nudge panel and toolbar. Pure GestureZoneGeometry follows edge insets, band confinement,
corner keep-out and quick-launch region. Direction labels use resources in all six locales.
HomeScreensTest exercises controls through the overlay; frame chips use filled Material colours.

## Notification presentation

NotificationSheet owns live state; NotificationSheetContent is the real presentation with an
injectable controller. NotificationChat keeps provider order while grouping authors and pauses.
MessageBubble and NotificationDismissBox use Material swipe dismissal; stable message keys keep
hiding local to the chosen bubble. NotificationHistory follows newest messages until scrolled away.
NotificationReply keeps action/result-key identity through updates and requires explicit retargeting.
Provider metadata supplies action icons, and large pictures decode off-thread up to 1024px.
NotificationScreensTest covers overview, conversation, replies and invented image attachments.

## Setup defaults and ordering

WidgetAppPrefs adds default_widget_clock_app, default_widget_calendar_app and default_widget_weather_app
in the existing DataStore and typed backup format. Saved clocks take precedence; setup updates an
already-bound initial clock. AccessStep and FavoritesStep are separate modules and reuse widget pickers.
ReorderableRows uses the existing reorderable library plus Material up/down controls; RowOrder is pure.
The setup-wide extra clickable modifier was removed because it consumed drag-handle down events.
SetupScreensTest verifies persisted order after both an arrow change and a simulated drag.
