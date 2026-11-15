# Components

Look here before writing any composable. If the row you need exists, use it; if it does not, search
Material 3 / Material 3 Expressive first, then the AniSync reference, and only then add it to `ui/kit/`.

| Need | Use | Where |
|---|---|---|
| Button, tonal, outlined, text | `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton` | Material 3 |
| Icon-only action | `IconButton`, `FilledTonalIconButton` | Material 3 |
| Related actions side by side | `ButtonGroup`, `ToggleButton`, `SplitButton` | Material 3 Expressive |
| Settings rows | `SettingsGroup { item / switchItem / block }` → `SegmentedListItem` | `ui/kit/settings` |
| Settings screen | `SettingsScaffold` → `LargeFlexibleTopAppBar` | `ui/kit/settings` |
| Section label | `SettingsSectionLabel` | `ui/kit/settings` |
| Popup menu | `Menu { item / divider / gap / scrollable }` → `DropdownMenuPopup` | `ui/kit/menu` |
| Dropdown field | `MenuField` | `ui/kit/menu` |
| Bottom sheet | `ModalBottomSheet`; `ViewportSheet` for the shared calendar/notification full viewport and compact handle | Material 3 / `ui/kit/sheet` |
| Dialog | `AlertDialog` | Material 3 |
| Navigation / content swap | `SharedAxis` for directional navigation; `FadeThrough.contentTransform()` for content | `ui/kit/motion` |
| Pressed feedback on custom content | `Modifier.bouncyClickable` | `ui/kit/motion` |
| Animation specs | `AppMotion.spatial() / effects() / …` | `ui/kit/motion` |
| Drag-to-reorder lists | `sh.calvin.reorderable` | library |
| Shapes | `MaterialTheme.shapes`, `MaterialShapes`, `*Defaults.shapes()` | Material 3 |

`ProjectRulesTest` fails the build on new hand-drawn rounded shapes, new user-visible string literals,
non-English text outside `res/values-<lang>` and files over 400 lines. Its allow-lists only shrink.
