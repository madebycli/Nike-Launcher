# Design principles

Nike Launcher should feel finished: calm, direct and generous. A change that breaks one of these is not done.

1. **The interface does not explain itself.** No hint banners, no "tap here" text, no extra button for
   something a gesture already does. Things are changed where they are, in one small window: tap the icon to
   change the icon, tap the name to change the name. What cannot be done is removed, not apologised for.
2. **Things sit where they are put.** Each home block (widget, now playing, favorites) is held at its own
   position, set by dragging with guides to settle on. Blocks never overlap; content that grows pushes only
   what it would otherwise cover, animated.
3. **Native components only.** Material 3 Expressive supplies buttons, rows, menus, sheets, tabs and their
   shapes and motion. Nothing is re-drawn. See `COMPONENTS.md`.
4. **Motion is shared.** Only the theme's motion scheme through `AppMotion`. A finger always wins: what is
   dragged follows it every frame. Entering and leaving use the same element.
5. **It works at the edges.** 320 dp wide, font scale 1.5, German-length labels, keyboard open, light and dark.
6. **Settings** are grouped by the surface they act on, one row style everywhere, every key searchable and
   part of the backup.
