# Victoria Grid comparison

Reference: `madebycli/victoriagrid`, tag `v0.2.0-preview.12`, commit
`f6c370c4aea23a4d208e82edd2debc8c02928a23`. The reference checkout is read-only.
Clock settings were compared against the tagged source; notification reference PNGs committed in
that tag were also opened. The reference app was not executed in an emulator.

## Priority feature audit

| Area | Nike implementation and verification |
|---|---|
| Clock settings structure | Home, Clock, Styles, Calendar, Weather and Appearance; actual pages rendered by `ClockConfigScreensTest` |
| Pinned preview | Fixed one-third / 120-240dp viewport; actual `ClockHeader`, contrast-selected Material plate in both themes |
| Clock styles | All 20 styles, grouped gallery, selected-style preview, selected-state contrast; every gallery row rendered |
| Time format | System, 12-hour and 24-hour choices; same stored enum values |
| Date | All existing formats and custom entry; off/on retains the chosen format; state restoration and save exercised |
| Calendar | Permission action, visible-calendar selection, event count, lookahead, relative/absolute time and calendar app; fake calendar provider used for renders |
| Additional rows | Battery, next alarm and notification count retained |
| Appearance | Time/date/calendar/battery/weather/notification size and opacity, text colour/reset, text width and clock-to-data gap retained; exact entry rejects invalid numbers |
| Weather settings | Enabled state, Celsius/Fahrenheit, selected app, forecast and provider status retained |
| Home editing | Independent reserved fields, grouped selection/movement/spacing, per-field edge distance and persistent drafts; actual edit controls exercised beneath gesture annotations |
| Gesture zones | Left/right/both edges, band-only/full-height zones, system inset and corner keep-out geometry; quick-launch app names and configured region; up/down direction markers |
| Inline app notifications | One shared Material app/age/title/message row on home and app list; right swipe and menu open the app overview even for one notification; no inline dismissal; profile isolation tested |
| Notification overview | Actual sheet rendered with invented snapshots; Material swipe dismissal and explicit clear/dismiss controls |
| Conversation | Consecutive-author grouping and pause timestamps, incoming/outgoing sides, newest-message anchoring, hide action, inline attachments and provider actions |
| Reply | Material field, suggestions, multiple input options and target-bound draft; replacing a provider action disables send until explicitly selected |
| Pictures | Inline content URI fixture and large-picture fixture rendered; large-picture decoding no longer uses avatar resolution |
| Corner button | Complete settings reachable while disabled; separate draft placement/reset/Done route and inset-aware normalized position; actual navigation and German font scale 1.5 renders |
| Search presentation | Exclusive None/Button/Bar, preserved legacy keys, independent button-field edge and matching slide origin; colours/opacity retained; actual search entry and intermediate transition positions checked |
| Widget side pages | Existing two-column grid, selection, size dialog, move/remove/settings actions; real grid rendered and resize confirmation exercised; pure drag/resize logic remains covered by its existing tests |
| Setup access | Durable page before external role request; activity recreation stays on Access; clock/calendar/weather app choices reuse widget pickers; OEM role dialog remains a phone check |
| Setup style | Fixed two-app preview is the first outer-scroll item; both requested icon sizes and scroll-away position asserted in light/dark/large text |
| Setup favorites | Same Material segmented rows for selected/candidate apps, no count or arrow pair; tonal remove/drag controls; equal row height and stored library drag order checked |
| Backup/restore | Real exporter/restorer test includes default apps, frame offsets, region, search colours, corner coordinates, independent button-field edge and clock configuration under a newly allocated widget id |
| Popup and submenu motion | Shared calendar/weather/notification viewport and compact stock handle; lower reply overflow and rounded Material Send; expressive widget/app menus; shared full-container navigation tested in both directions |
| Weather sheet header | Actual overview/daily/hourly sheet rendered in German at font scale 1.5; the existing flexible title width fits the Pixel 9 viewport |
| Home performance | Measured band-state recomposition scope, then isolated it; `docs/PERFORMANCE.md` records the limits |

## Resource-name audit

The tagged reference contains 426 English resource keys. Nike contains 640. The 94 reference key
names absent from Nike were reviewed by area; absent names do not by themselves indicate absent
behaviour. Existing Nike keys were kept stable.

- `handle_*` and `edit_value_range`: the reference's layout editor labels correspond to Nike's
  `NudgePanel`, `edit_move_*`, `edit_edge_distance` and `edit_value_dp` resources.
- Old widget show/hide chip labels (`widget_clock_*_show`, `*_hide`, colour-pick wording) correspond
  to the enabled-state rows on the six widget settings pages. The underlying settings remain.
- `onboarding_*`, `welcome_*`: Nike uses `setup_*`, the shared favorites chooser and widget app
  picker resources. The restore entry, permissions, style preview, icon packs, side pages,
  favorites order and app choices were checked. Weather-provider connection remains reachable
  through the existing weather sheet connection action.
- `search_button_colors`, colour names, frequent-count and A-Z visibility labels use Nike's
  existing settings/colour resources and enum choices. Background-image selection and error
  handling remain in the app-list background settings and image picker.
- Introductory paragraphs, tutorials and `*_hint`/explanatory `*_detail` strings were not added,
  following the project's no-explanatory-UI rule.

All English resource keys now have matching entries in de/es/pl/ru/sv. The clock-settings work
filled 39 missing strings in each of es/pl/ru/sv; gesture labels and nine new corner/search labels are present in all six locales.

## Verification boundaries

Renders use the real Nike composables at 1080 x 2424px / 420dpi, with invented apps, calendars,
notifications and weather data. They establish layout and state after the simulated actions.
They do not establish animation timing, physical swipe velocity, wallpaper integration, installed
icon-pack quality, live notification delivery, Android role/bind dialogs or frame times on a phone.
The generated `perf` APK is intended for those remaining Pixel 9 checks.

The follow-up uses actual app navigation for corner recovery/placement and notification entry on both
home and app list. The app-list walkthrough also renders menus with and without notifications.
MenuMotionScreensTest samples measured transition positions with the test clock; it does not measure
phone animation smoothness. Setup screenshots wait for the persisted page before capturing.
