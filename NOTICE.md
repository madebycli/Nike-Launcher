# Notices and third-party sources

Nike Launcher is licensed under GPL-3.0-or-later (see [LICENSE](LICENSE)). Every licence below was
checked against the file it names: upstream `LICENSE` files, font `name` tables and the licence
files inside the built APK. Full licence texts are in [licenses/](licenses/).

## 1. Upstream code taken over

| Source | Licence | Where it is in this repository |
|---|---|---|
| [Victoria Launcher](https://github.com/adelmonte/victoria-launcher) by adelmonte | GPL-3.0-or-later | Fixes and structure ported in upstream merges 4844a23 and 5706d50, e.g. `app/src/main/baseline-prof.txt`, `data/PrefsSalvage.kt` (header names the origin), `data/AppInfo.kt`, `data/Prefs.kt`, `ui/home/**`, `ui/applist/**`, `widget/WidgetPickerActivity.kt` |
| [Orion Store](https://github.com/RookieEnough/Orion-Store) by RookieEnough and contributors, commit e561e91 | GPL-3.0 | `ui/kit/orion/*` (each file names the Orion source in its header) |
| [AniSync](https://github.com/Marco-9456/AniSync) by Marco-9456 | GPL-3.0-or-later | `ui/kit/motion/*` (`AppMotion`, `BouncyClickable`, `FadeThrough`, `MotionConstants`, `SharedAxis`), `ui/kit/menu/*` |

Victoria Launcher files carry the origin in their header only where it was written; the list above is the
complete set of files that came from that merge.

**Not verified in this release, please confirm before publishing:**

- **iris-android** (named in the header of `ui/kit/orion/OrionPageScaffold.kt` as the port source). The
  repository and its licence could not be checked. Until confirmed, only the Orion source is treated as the
  licence basis for that file.
- **Victoria Grid** (`madebycli/victoriagrid`), the reference for the notification and clock structure.
  Its own licence is not checked here.

## 2. Fonts (`app/src/main/res/font/`)

All seven fonts are under the SIL Open Font License 1.1 and are shipped unmodified. Licence texts:
[licenses/fonts/](licenses/fonts/).

| File | Family and copyright | Licence text |
|---|---|---|
| `google_sans_flex.ttf` | Google Sans Flex, Copyright 2015 Google LLC. Its name table marks "Google Sans" as a trademark of Google. | `GoogleSansFlex-OFL.txt` |
| `kumbh_sans_semibold.ttf` | Kumbh Sans, Copyright 2020 The KumbhSans Project Authors | `KumbhSans-OFL.txt` |
| `lexend.ttf` | Lexend, Copyright 2019 The Lexend Project Authors | `Lexend-OFL.txt` |
| `monoton.ttf` | Monoton, Copyright 2011 vernon adams. Reserved Font Name "Monoton". The TTF has no licence field; the OFL text is taken from the [Google Fonts repository](https://github.com/google/fonts/tree/main/ofl/monoton). | `Monoton-OFL.txt` |
| `nunito_sans_vf.ttf` | Nunito Sans, Copyright 2016 The Nunito Sans Project Authors | `NunitoSans-OFL.txt` |
| `press_start_2p.ttf` | Press Start 2P, Copyright 2012 The Press Start 2P Project Authors. Reserved Font Name "Press Start 2P". | `PressStart2P-OFL.txt` |
| `rubik_glitch.ttf` | Rubik Glitch, Copyright 2020 The Rubik Filtered Project Authors | `RubikGlitch-OFL.txt` |

Reserved Font Names only restrict modified versions. These files are unmodified.

## 3. Libraries

**Shipped in the APK** (resolved from the Gradle cache, licence from each POM): AndroidX, Jetpack Compose,
Material 3, Navigation, DataStore, Kotlin stdlib, kotlinx-collections-immutable, reorderable and
Material Icons Extended are **Apache-2.0**; the protobuf runtime inside DataStore is **BSD-3-Clause**.
The APK ships the licence files of the AndroidX libraries under `META-INF/`. The protobuf text is in
[licenses/BSD-3-Clause-protobuf.txt](licenses/BSD-3-Clause-protobuf.txt); the Apache text is in
[licenses/Apache-2.0.txt](licenses/Apache-2.0.txt).

**Build and test only (not in the APK):** JUnit 4 (EPL-1.0), Robolectric (MIT, text in
[licenses/MIT-Robolectric.txt](licenses/MIT-Robolectric.txt)), org.json (declared public domain in its
2023 release, text in [licenses/JSON-org-public-domain-notice.txt](licenses/JSON-org-public-domain-notice.txt)),
AndroidX test libraries (Apache-2.0), and the Gradle wrapper (Apache-2.0).

One resolved artifact, `com.google.guava:listenablefuture`, has no licence block in its POM. It does not
appear in the APK.

## 4. Screenshots and images

`docs/screenshots/` and `fastlane/metadata/android/en-US/images/` show Nike Launcher's own interface with
placeholder app icons and invented text. The store phone screenshots that showed a third-party wallpaper
were left out of this snapshot; see the open items below.

## Open items before a public release

1. Confirm the iris-android repository and its licence (section 1).
2. Confirm the Victoria Grid licence (section 1).
3. Re-add store phone screenshots only with wallpapers whose source and licence are confirmed, or with
   screenshots of Nike's own renders.
