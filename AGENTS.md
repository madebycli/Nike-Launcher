# AGENTS.md — Nike Launcher

Read this whole file before you change anything. The first section is binding for every change, large or small.

## Non-negotiable rules

### 1. Use the prebuilt component. Never rebuild one.

Nike Launcher is a Material 3 Expressive app. If Material 3, Material 3 Expressive or the AniSync kit
(`ui/kit/`, see `docs/COMPONENTS.md`) already ships the component, use it. Do not hand-draw it, do not
re-implement its shape, corner radius, colour, state layer, motion or layout.

- Buttons are `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `IconButton`, `ButtonGroup`,
  `ToggleButton`, `SplitButton`, `FloatingActionButton`. Their shapes and sizes come from `ButtonDefaults`.
  Never write `RoundedCornerShape(...)` on a button, a chip, a card, a sheet, a menu or a text field.
- Tabs, segmented controls, chips, switches, sliders, search bars, top bars, navigation bars, menus,
  dialogs, bottom sheets, progress indicators, loading indicators: use the Material 3 / M3E component.
- Shapes come from `MaterialTheme.shapes` or `MaterialShapes` / `ButtonDefaults.shapes()` and friends.
  Motion comes from `MaterialTheme.motionScheme` through `ui/kit/motion/AppMotion`. Colours come from
  `MaterialTheme.colorScheme`. Type comes from `MaterialTheme.typography`.
- Before you write any composable that draws a surface, a pill, a card, a handle, a menu row, an icon
  badge or a picker, search these in order: (1) androidx Material 3 / M3E, (2) `ui/kit/`, (3) the
  AniSync reference clone (`Marco-9456/AniSync`, GPL-3.0). Only if all three have nothing, write it, in
  `ui/kit/`, with a one-line comment saying what was searched and why nothing fit.
- No workarounds. If the right component does not do what you need, fix the usage or ask; do not paper over
  it with a custom look-alike. A look-alike found in review is deleted and replaced, not tolerated.
- Prefer AniSync code over new code: copy the component, keep its structure, adapt names and strings,
  and keep the attribution header (see "Third-party code" below).

### 2. Everything in this repository is English

Code, comments, identifiers, commit messages, docs, issue and pull-request text, log messages, test names:
English only. No German (or any other language) outside `res/values-<lang>/`. Do not mention the projects this
one was derived from or compared against except where a licence requires attribution. Victoria Grid may be
named as a design/logic reference where it helps; no other project may be described as a model or a rival.

### 3. No monoliths

- A source file stays under about 400 lines. Past 600 it must be split before anything else is added.
- One concept per file, one feature per folder. A screen is a folder: `FooScreen.kt` (wiring), `FooState.kt`,
  `FooViewModel.kt`, and `components/` for the parts it is made of.
- Settings, preferences and models are split per area (`data/prefs/<Area>Prefs.kt`), never one grab-bag.
- Pure logic (layout maths, ordering, parsing) lives apart from Compose so it can be unit tested; add the
  test in the same change.
- Upgradeability is the point: moving one button, renaming one setting or swapping one component must
  touch one file.

### 4. Text is data, never code

- No user-visible string literal in Kotlin. Every string is a resource in `res/values/strings.xml`
  (English, the source language) and, when available, `res/values-<lang>/strings.xml`. Use `stringResource`,
  `pluralStringResource`, never string concatenation for sentences.
- German and other languages run 30–40 % longer than English. Every label must survive that: allow wrapping
  or `maxLines` with ellipsis deliberately, never fixed widths for text, no clipped buttons. Test with the
  longest translation and with font scale 1.5.
- Translation keys are stable. Renaming a key is a breaking change for translators; add a new key instead.

### 5. The interface does not explain itself

No hint banners, no tutorial text, no "tap here to…", no apologies, no beta disclaimers. An icon or a
single word is enough. Editing happens in place, in the same small window, by touching the thing being
edited. This launcher is for people who already know how a launcher works. See `docs/DESIGN-PRINCIPLES.md`.

### 6. Honest verification

Say what was run and what was seen. A green unit test does not prove an animation is smooth; a standstill
screenshot does not prove a gesture works. State plainly what could not be checked.

## Project facts

- Android launcher, Kotlin, Jetpack Compose, Material 3 Expressive. Package `dev.nikelauncher`.
- Build: Gradle with a version catalog (`gradle/libs.versions.toml`), JDK 17+, Android SDK platform 37.
  `./gradlew testDebugUnitTest lintDebug assembleDebug`.
- No `INTERNET` permission unless a feature explicitly needs it and `docs/ARCHITECTURE.md` says so.
- `docs/ARCHITECTURE.md` lists the module layout and the things that look wrong until you know why. Update it
  with the code, in the same commit.
- Every animation change needs an entry in `docs/animation-manifest.json` once that file exists.

## Third-party code

AniSync (https://github.com/Marco-9456/AniSync, GPL-3.0) is the visual and code reference. When a file is
taken over from it:

1. Put it under `ui/kit/` (or the feature folder that uses it), renamed to this project's package.
2. Keep a header: `// Adapted from AniSync (GPL-3.0-or-later), <original path>.`
3. Strip what Nike does not need (accounts, GraphQL, media types) instead of leaving dead code.
4. Add the file to `NOTICE.md`.

The product name AniSync, its logo and its strings are not used in the UI.

## Working rules

- Small commits with an imperative subject; one logical change each. No generated build output in git.
- Run the unit tests and lint before you push. Never push a red build.
- Never commit signing material (`keystore.properties`, `*.keystore`).
- Settings keys are persisted data: never rename or retype one without a migration and a backup-format note.
