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
  prefs/SearchPrefs.kt           exclusive search mode using legacy booleans; separate button-field edge
  prefs/SetupPrefs.kt            durable first-run page before external home-role requests
  prefs/WidgetAppPrefs.kt        default clock/calendar/weather apps for newly created clock widgets
  prefs/HomeLayoutPrefs.kt       atomic frame pins, shared block gaps and selected-field edge distances
  Folder.kt                     folder model + JSON persistence + favorites token encoding
  HomePins.kt                   where each home block is held (edge + fraction)
  WidgetPages.kt                the grids of widgets on the pages either side of home
  CornerButton.kt               the corner button's actions, shapes and colors
ui/
  NikeNavHost.kt                collects settings once, hosts the navigation graph, shows the first-run setup
  home/HomeRoute.kt             home destination: pages, app-list overlay, edge zones
  home/route/                   list host, wallpaper layers, page host, gesture exclusions; ObserveScrubBand isolates band subscriptions
  home/stack/, blocks/, rows/   independent reserved frames, their contents, favorite and folder rows
  home/scroll/                 pure elastic-drag boundaries; HomeScroll owns synchronous input and one settling job
  home/edit/                    frame selection/draft, grouped placement, nudge controls and the Material home-layout settings sheet
  home/pages/                   widget pages: pager, grid, resize and reorder; components/WidgetGridToolbar uses stock button defaults
  home/corner/                  the corner button, its sheets, a pinned CornerSettingsPreview, separate placement canvas/geometry and the floating search button
  home/setup/                   the first-run flow; StyleStep scrolls its fixed two-app StylePreview together with the controls
  home/wallpaper/               system blur, picture and grain behind the app list
  applist/                      screen parameters/session, placement effects, nested pull connection, scene, rows, search slots, strip and scrub state
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
state-based Material text-field API. The preview reserves a fixed 120-240dp viewport rather than
resizing to the current text. PreviewContrast chooses among Material surfaces by measured contrast,
so white text keeps a dark plate in either theme. The first-run style preview reserves 264dp for two complete 96dp icons and the maximum row gap.
It is the first item of the outer settings scroll, so it leaves room for controls when scrolled away. It reuses AlignedIconLabel so large/right-side icons
keep their requested size and cannot be squeezed by labels or earlier rows.

**Setup uses the same app pickers as widget settings.** `WidgetAppPrefs` stores initial app choices
in the main DataStore, so they travel in the existing typed backup format. The three new keys are
`default_widget_clock_app`, `default_widget_calendar_app`, and `default_widget_weather_app`.
Existing saved widget configurations take precedence when reopened. Choosing an app during setup
also updates an already-bound first-run clock. `AccessStep` and `FavoritesStep` have separate files.

**Setup needs only the Material Surface to block touches behind it.** An additional empty clickable
modifier on the entire setup consumed the down event needed by reorder handles. Favorite rows now
use `SelectedFavorites`: the existing `sh.calvin.reorderable.ReorderableColumn` with the same
segmented Material rows as the available apps. Two tonal icon buttons remove and drag; accessibility
reorder actions remain available without visible arrow pairs. Ordering stays in `RowOrder.kt`.

**First-run progress is durable before leaving for Android.** `SetupPrefs` writes `setup_step`
(0-4) to DataStore. `SetupFlow` waits for that value instead of briefly drawing Welcome. A fresh HOME
task or process therefore resumes Access after the home-role chooser; finishing still uses the
existing `welcome_seen` flag. This is a simulated lifecycle regression, not an OEM role-dialog test.

**Corner placement is independent of home editing.** Home settings always reach the complete
button settings, including Enabled. `corner/place` draws home and a separate full-screen placement
canvas, with Reset and Done. A saved normalized `CornerPosition` maps through pure inset-aware travel
geometry. Reset changes only the draft; Back cancels, Done writes the pair atomically. The optional
float keys `corner_position_x/y` are absent for the original corner alignment.

**Search presentation has one owner.** `SearchPrefs` derives None/Button/Bar from the existing
`applist_search_enabled` and `search_button_floating` booleans, preferring Bar for legacy conflicts.
Choosing a mode writes both keys atomically. `search_button_at_bottom` (boolean, default true) is
independent of the existing permanent-bar edge. Hidden-app search retains its original meaning.
The button and corner focus request reveal the field before focusing it; `AppListSearchSlot` uses
Material slide/fade motion from the configured edge. A temporary button search closes when the keyboard has
been visible, is closed and the query is empty; permanent search bars and nonempty queries remain. No key was renamed or retyped; the optional
corner, search and setup keys remain supported by typed backup format 2.

**Settings navigation shares the kit motion.** `SharedAxis` adapts the full-container forward/back
slide and fade; both NavHost destinations and inline settings sections use it. Widget and app-row
menus use `TouchMenu` and expressive Material grouped-menu shapes. Attribution is in NOTICE.md.
See `animation-manifest.json` for changed transitions and their verification boundaries.

**A home block owns its field even when its contents disappear.** When Now Playing is enabled,
its fixed-height field remains in the stack during playback, pause, dismissal and silence. Only
content inside that field changes. Edit mode does not add padding to measured frames or insert a
missing widget placeholder. Adding a widget remains available in the menu and toolbar.
Pins are authoritative rather than pushed by preceding blocks. Short pinned fields are clamped
inside the usable viewport; oversized content can still scroll. Content extent is the furthest
frame bottom plus the navigation inset, not the last enumerated frame or the outer constrained box.

MediaDismiss wraps the audio card in the stock Material SwipeToDismissBox. A short, interrupted or
cancelled swipe rebounds under Material state ownership; there are no per-event launched snaps.
A new MediaSession receives fresh dismissal state. HomeLayoutScreensTest checks the title's actual
horizontal position after a partial/cancelled swipe before verifying full swipe dismissal.

**Editing uses the latest settings and an acknowledged draft.** PinEditor reads current data/actions
through updated references, freezes unpinned neighbours when placing fields, and keeps pending moves
visible after Done until their encoded pin values match the store. Cancellation restores the earlier
draft. Frame chips support multiple selection; vertical drags and nudges move that group together.
HomeEditControls owns the controls and HomeLayoutSheet uses Material settings groups/sliders for
block gaps, favorite-row spacing and common selected-field edge distances. Batch pins and edge edits
are atomic. Changing a shared padding fallback first preserves unselected fields' inherited values.
Nudge buttons implement ordinary clicks and hold-repeat; fractional pixel carry prevents repeated
one-dp steps drifting on the Pixel 9's 2.625 density. Editor controls never change frame dimensions.

The new persisted keys are `home_block_gap_dp` (default 12) and `home_media_side_padding_dp` (falls
back to the existing side padding until explicitly changed). Existing pin, padding, offset and row
spacing keys keep their names and types. All keys remain covered by typed backup format 2.

**Home dragging has one animation owner.** Pointer deltas update the offset synchronously; a single
settling job is cancelled before the next drag. Elastic travel is bounded and returns to the same
boundary on release or cancellation. Real overflowing content retains ordinary scrolling. The
settling spec comes from the Material motion scheme. Pull thresholds use undamped travel; a deep
pull requests the existing notification-shade action once. Android still requires its existing
accessibility-service route on builds that block reflection. Tests exercise cancellation, interrupted
rebound, swipe-up completion and the actual accessibility global-action request under Robolectric.

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

**Notification rows replace the normal label, without repeating the app icon.** Both favorites
and app-list rows use the same flat Material `NotificationAppContent`, following the Grid preview's
app/age/title/latest-message hierarchy. `AppNotification` excludes shortcuts, work/private profiles
and other users before subscribing. A right swipe opens the app overview even with one notification;
optional shortcut swipes never override it. Inline rows cannot dismiss notifications. Folder excerpts
share the same eligibility check and row gesture. The app row owns its sheet outside the optional
preview, so provider removal cannot dispose it mid-close. Shortcut swipes default off; explicit
stored legacy/user choices survive. The overview uses stock Material icon buttons: DeleteSweep
for clearing all, DeleteOutline per notification, and Close only for the sheet. Accessible names reuse the existing localized resources;
there is no visible Clear all text button. Clearing all uses a click-time snapshot so synchronous
provider updates cannot skip an entry. Notification details put Open, provider actions and explicit
dismissal in one kit overflow beside the reply field. Calendar/weather and notifications share `ViewportSheet`: full available
height below the status bar/cutout, one 24dp stock-handle slot and the same bottom/horizontal insets.
There is no separate Open row between history and reply. The send symbol is the stock rounded
Material Send icon. The state-based reply field grows to four lines, then scrolls internally, while
retaining its explicit action/result-key target and 4096-character bound.

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
| `REQUEST_DELETE_PACKAGES` | Lets Android present its confirmation when the user selects Uninstall; the launcher does not silently remove apps. | No |
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

The Now Playing visibility control is in Home settings. Adjusting app-list dimming does not
trigger the home wallpaper preview, so the settings surface stays opaque.

Notification previews default on for eligible apps when no preference has been stored; an explicit
opt-out remains authoritative. The notification listener permission and existing profile gates still apply.

Notification rows derive their one-line title and message size from the app label, matching the
reference ratio. The old independent notification-size preference remains stored for backup
compatibility, but its control is no longer offered.

The media widget has Classic and Expressive styles. Its `media_widget_style`,
`media_widget_controls`, and `media_widget_progress` preferences are included in the normal
settings backup. Existing installations default to Classic with controls and without progress.
The progress indicator uses the active media session's duration and position; it is omitted
when the session does not provide a usable duration.

The navigation host keeps the loaded widget-page configuration while settings are open, so
the home page's initial index is known when returning. A screen-off broadcast resets the
widget pager and unwinds launcher navigation without the usual pop transition.

Third-party widget host views enforce the system app-widget background radius, clipping to
the provider background when available. The launcher's own transparent clock is unchanged.
A visible clock with weather enabled silently checks the configured local provider when its
cache is missing or stale; provider permission and location checks remain authoritative.
The bottom search field follows the IME inset directly and enters/exits from below. The app-list
slot owns this transition; the search field itself does not add a second entrance animation.

The status bar starts hidden while the stored visibility setting is read, then follows that setting.
A zero-second hide applies the window's fullscreen policy immediately rather than waiting for
an animation controller; screen-off and resume reassert the hidden home state before focus.
The hidden app-list state retains fullscreen window policy instead of clearing it during the
transition from home. The edge-to-edge search layout uses IME insets for keyboard positioning;
fullscreen window policy prevents legacy adjustResize and needs an on-device keyboard check.
Navigation and status bar preferences remain independent.

When the system returns status-bar control after keyguard, and on the user-present broadcast,
the launcher reasserts a hidden home bar. It does not override visible home, active app-list,
or explicit peek state. Transient system-owned indicators are outside the app's window.

Icon selection and reset write their preference before popping the icon picker destination;
the destination's coroutine therefore cannot be cancelled by its own navigation.
# Search results and system bars

Search results reserve the top status-bar and display-cutout insets even when the app-list status
bar is hidden. Normal list positioning stays independent of this search-only safe area. Bar control
restoration uses the active surface's requested visibility; home and app-list preferences stay separate.
