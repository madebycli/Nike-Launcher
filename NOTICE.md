# Third-party notices

Parts of `app/src/main/java/dev/nikelauncher/ui/kit/` are adapted from AniSync
(https://github.com/Marco-9456/AniSync), GPL-3.0-or-later. Each adapted file names its origin in its header.

| File | Origin |
|---|---|
| `ui/kit/motion/AppMotion.kt`, `MotionConstants.kt`, `FadeThrough.kt`, `BouncyClickable.kt` | `presentation/util/` |
| `ui/kit/menu/Menu.kt`, `MenuScope.kt`, `ScrollingMenuRows.kt` | `presentation/components/menu/Menu.kt` |

`ui/kit/motion/SharedAxis.kt` adapts the forward/back settings transitions from AniSync
`presentation/settings/SettingsListDetail.kt` (GPL-3.0-or-later). Timing follows Material motion tokens.

`app/src/main/java/dev/nikelauncher/ui/kit/orion/` (OrionMotion, OrionTonal, OrionBento, OrionPageHero, OrionShared,
OrionSwipeRow) is adapted from Orion Store by RookieEnough and contributors
(https://github.com/RookieEnough/Orion-Store, commit e561e91, GPL-3.0), taken over by way of iris-android's
`components/orion/` port. Each file names its Orion source in its header.
