# Architecture

Nike Launcher is a single-module Android home-screen launcher: Kotlin, Jetpack
Compose, `minSdk 26`, `compileSdk`/`targetSdk 37`. No network permission, no
analytics, no ads, no accounts.

## Package layout (`dev.nikelauncher`)

```
MainActivity.kt                 Activity, window setup, status-bar peek
NikeApp.kt                  Application; owns Prefs, repositories, widget host
data/
  AppInfo.kt, AppRepository.kt  profile-aware activity keys, LauncherApps queries, launch and app-info intents
  IconPackRepository.kt         icon-pack discovery, appfilter.xml parsing, icon lookup
  Prefs.kt                      owns the DataStore; settings live in Library/Appearance/Home/AppList/Strip/Corner/WidgetPagePrefs.kt, keys in PrefKeys.kt
  backup/                       the backup file: typed values, widgets with new ids, files the settings point at, widgets left to confirm
  prefs/WidgetAppPrefs.kt        default clock/calendar/weather apps for newly created clock widgets
  Folder.kt                     folder model + JSON persistence + favorites token encoding
  HomePins.kt                   where each home block is held (edge + fraction)
  WidgetPages.kt                the grids of widgets on the pages either side of home
  CornerButton.kt               the corner button's actions, shapes and colors
ui/
  NikeNavHost.kt                collects settings once, hosts the navigation graph, shows the first-run setup
  home/HomeRoute.kt             home destination: pages, app-list overlay, edge zones
  home/route/                   list host, wallpaper layers, page host, gesture exclusions; ObserveScrubBand isolates band subscriptions
  home/stack/, blocks/, rows/   the pinned stack, its blocks, favorite and folder rows
  home/edit/                    edit mode: frames, drag handles, snap guides; NudgePanel moves the selected frame vertically and its clipped contents horizontally
  home/pages/                   widget pages: pager, grid, resize and reorder; components/WidgetGridToolbar uses stock button defaults
  home/corner/                  the corner button, its sheets, a pinned CornerSettingsPreview and the floating search button
  home/setup/                   the first-run flow; StyleStep pins a StylePreview (the favorites, read from the same prefs) above the style controls
  home/wallpaper/               system blur, picture and grain behind the app list
  applist/                      A-Z list, pull-to-collapse, strip and edge zone, scrub state
  notifications/                live sheet host, testable presentation, grouped message history, Material bubbles/swipe dismissal, provider actions, target-bound reply drafts and image loaders
  settings/                     settings screen, its sections and the picker screens
  theme/                        Material 3 Expressive theme, fonts, wallpaper-aware text color
  kit/                          shared building blocks: motion, menu, settings rows, onboarding (see docs/COMPONENTS.md)
media/                          notification listener, preview bus and parser, Now Playing
service/                        haptics, status-bar fader, accessibility service, system UI
widget/                         clock widget (model, faces, weather, calendar, overview sheets), widget host, picker, restore
  config/                       clock settings: screen/navigation, clock/calendar rows, appearance/weather rows, pinned preview, exact number input and dialogs
```

## Things that look wrong until you know why

**The launcher does not draw the wallpaper.** `Theme.NikeLauncher` sets
`android:windowShowWallpaper`, so the real system wallpaper shows through a
transparent window. "Dim wallpaper" is therefore just a scrim `Box`, not image
processing.

**Clock settings are a draft until Done.** The serializable configuration and last enabled date
pattern survive activity recreation. Disabling the date preserves its format for the next time it
is enabled. Every page uses the settings kit; the pinned preview uses the actual clock header.
The style gallery is the only page that replaces the preview with its own selected-style card.
Calendar selection and the style gallery live in separate files. Date and number entry use the
state-based Material text-field API.

**Setup uses the same app pickers as widget settings.** `WidgetAppPrefs` stores initial app choices
in the main DataStore, so they travel in the existing typed backup format. The three new keys are
`default_widget_clock_app`, `default_widget_calendar_app`, and `default_widget_weather_app`.
Existing saved widget configurations take precedence when reopened. Choosing an app during setup
also updates an already-bound first-run clock. `AccessStep` and `FavoritesStep` have separate files.

**Setup needs only the Material Surface to block touches behind it.** An additional empty clickable
modifier on the entire setup consumed the down event needed by reorder handles. Favorite rows now
use the existing `sh.calvin.reorderable.ReorderableColumn` and Material up/down buttons, with ordering
in the framework-free `RowOrder.kt`.

**A block frame and its contents have independent positions.** `HomePins` anchors a full-width frame
vertically. `BlockOffsets` stores each block's horizontal content offset in `block_offset_*`; content
is clipped to its frame. `NudgePanel` also changes row/widget edge padding through their existing
preferences. These keys are included in the settings backup.

**Edit-mode gesture annotations do not receive touches.** `HomeGestureZones` uses Canvas and Material
badges without pointer handlers, below the nudge panel and edit toolbar. `GestureZoneGeometry` follows edge side/insets/width, the optional
letter-band confinement, corner-button keep-out, and `QuickLaunchRegion`. Pull-down and swipe-up
labels describe directions on the home drag surface; they do not introduce new gesture hit zones.

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
`NotificationChat` groups consecutive authors without sorting provider messages. History follows
the newest message until the reader scrolls away. A draft retains its action and result key across
provider updates; changing a target requires an explicit selection. Large notification pictures
are decoded up to 1024px; avatar icons remain bounded at 128px. Both loaders run off the main thread.
Presentation is keyed by selected notification id, so content updates retain the draft and history.

**A backup carries more than settings.** Widgets get new ids on another phone, so each is bound
again and every reference (home, folders, widget pages) is remapped; a widget the system will not
bind by itself waits in `WidgetRestoreActivity`. Files the settings point at (a font, icons, the
list picture) travel inside the file, checked before anything is written.

## Recomposition

The moving state has narrow subscribers:

- **`ScrubState`** holds the letter, position and elastic pull. It is passed down as
  one stable object so only the composable that *reads* a field is invalidated. When
  the home destination read the letter directly, each of the ~26 letter changes per
  gesture invalidated that whole scope — and the home screen hangs off it.
- **Icons are rasterised once into a byte-bounded `LruCache`** and pre-warmed off the
  main thread, favorites first. The cache is bounded by bytes rather than entry
  count because the cache key includes the rasterised pixel size: moving the
  icon-size slider adds a generation rather than replacing one, and a single 96dp
  icon on a dense screen is ~450 KB.
- **`ObserveScrubBand`** reads `BandEditing.band` inside the consumers rather than `HomeRoute`.
  The hidden app list does not observe the moving band; it reads the latest value when revealed.
  The focused 120-update probe in `BandRecompositionTest` records 121 outer compositions before
  isolation and 1 afterwards, with 121 visible-consumer compositions and 1 hidden-consumer
  composition. These are snapshot/recomposition counts, not phone frame-time measurements.

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

- Double-tap-to-lock is handled by the app list/edge zone. The bare home wallpaper has no double-tap handler.
- No generated Baseline Profile. Recording and validating one still needs a device.
- Robolectric verifies real Compose layouts and simulated interactions. Device gesture velocity,
  notification-provider delivery, system-role dialogs, wallpaper and frame timing need phone checks.

## Elastic home input

HomeScroll applies pointer deltas synchronously and owns one cancelling/settling animation using
the Material motion scheme. HomeStack reports actual content height rather than the constrained
outer box. Short content rebounds to its boundary, including cancelled/interrupted gestures;
real overflow still scrolls. Deep pull uses undamped travel and requests the existing shade action
once. HomeScrollInteractionTest drives the real draggable/clock and records the accessibility
global-action request through SystemUi with a Robolectric service; DragMathTest covers boundaries.
