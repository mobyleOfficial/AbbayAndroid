# Credits

Every third-party library and every asset (icon, font, sound, image, sample audio) used by Abbay is listed here with its source and license. Entries are required before an asset or dependency is merged.

The full, generated list of shipped libraries (including transitive ones) is produced by `./gradlew generateLicenseReport` in `app/build/reports/dependency-license/`; `./gradlew checkLicense` fails the build on a license outside `config/licenses/allowed-licenses.json`. The app's About → Open-source licenses screen is generated at build time from that report and from the *Bundled source code* and *Assets* tables below — keep their column order (name, source, license).

## Libraries
| Library | Version | License | Use |
|---|---|---|---|
| AndroidX Core, Lifecycle, Activity Compose, Startup, DocumentFile, Core Splashscreen | see `gradle/libs.versions.toml` | Apache-2.0 | Platform support |
| Jetpack Compose (UI, Material, Material 3, icons), ConstraintLayout Compose, Navigation Compose | see `gradle/libs.versions.toml` | Apache-2.0 | UI |
| AndroidX Media3 (`exoplayer`, `session`, `ui`, `common`, `extractor`, `cast`) | 1.4.1 | Apache-2.0 | Playback, media session, notification icons (skip 5/10/15/30 s), MP3 duration scan |
| AndroidX MediaRouter, AppCompat (`:cast` only) | 1.7.0 / 1.7.0 | Apache-2.0 | Cast route button and its dialogs |
| Google Play services Cast framework (pulled by `media3-cast`, `:cast` only) | see dependency report | Android SDK license (platform service) | Cast sender |
| AndroidX ProfileInstaller | 1.3.1 | Apache-2.0 | Installs the Baseline Profile |
| AndroidX Benchmark (Macrobenchmark), UI Automator (`:benchmark` only, not shipped) | 1.3.3 / 2.3.0 | Apache-2.0 | Startup and scrolling benchmarks, Baseline Profile generation |
| AndroidX Room | 2.6.1 | Apache-2.0 | Database |
| AndroidX DataStore (Preferences) | 1.1.1 | Apache-2.0 | Settings |
| AndroidX WorkManager | 2.10.0 | Apache-2.0 | Background work |
| AndroidX Glance (`appwidget`) | 1.1.1 | Apache-2.0 | Home screen widgets |
| Hilt (Dagger), AndroidX Hilt | 2.52 / 1.2.0 | Apache-2.0 | Dependency injection |
| Kotlin coroutines, kotlinx.serialization | 1.7.3 / 1.4.1 | Apache-2.0 | Concurrency, JSON |
| Coil (compose, svg) | 2.7.0 / 2.4.0 | Apache-2.0 | Image loading |
| Glide Compose | 1.0.0-beta01 | BSD-2-Clause + Apache-2.0 | Image loading (to be consolidated on Coil) |
| Material Components for Android | 1.12.0 | Apache-2.0 | Theme |
| Robolectric, Roborazzi (test only) | 4.14.1 / 1.32.2 | MIT / Apache-2.0 | Screenshot and JVM UI tests |
| Firebase Crashlytics, Firebase Analytics | 18.6.2 / 21.5.1 | Apache-2.0 SDKs; Google service terms | Crash reports, usage counters |

## Bundled source code
| Code | Author / source | License | Location |
|---|---|---|---|
| RNNoise, vendored C sources, tag v0.1.1, commit 6cbfd53 | Xiph / Mozilla, Jean-Marc Valin | BSD-3-Clause (`app/src/main/cpp/rnnoise/COPYING`) | Noise reduction; built with the NDK into `libabbay_rnnoise.so` with its built-in model |

## Assets
| Asset | Author / source | License | Location |
|---|---|---|---|
| App icon, launcher foreground/background, `logo.png` | Abbay project (pre-existing in AbbayAndroid, owner-provided) | Abbay trademark, not MIT | `app/src/main/res/mipmap-*`, `drawable/ic_launcher_background.xml`, `drawable-xxxhdpi/logo.png` (scaled to 576 px for the splash) |
| `file_music`, `file_plus`, `folder_plus`, `ic_book`, `ic_clock`, `ic_play`, `ic_stop`, `img_no_permissions` vectors | Abbay project (pre-existing in AbbayAndroid, owner-provided) | MIT (project asset) | `app/src/main/res/drawable/` |
| `vbr_no_xing.mp3`, `vbr_with_xing.mp3` test fixtures | Abbay project: synthetic silence/tone/noise generated in Python, encoded with LAME through ffmpeg as a build-time tool (no encoder code ships) | CC0 1.0 | `app/src/test/resources/` |
| `ic_sleep_timer`, `ic_sleep_timer_off` vectors (notification sleep-timer button) | Abbay project, drawn for Phase 4 | MIT | `app/src/main/res/drawable/` |
| Sleep-timer chime and shake confirmation tones | Abbay project: sine tones synthesized at run time in `TonePlayer` (no audio file) | MIT | `app/src/main/java/com/mobyle/abbay/presentation/playback/sleep/TonePlayer.kt` |
| `ic_shortcut_resume`, `ic_shortcut_sleep`, `ic_tile_play` vectors (launcher shortcuts, Quick Settings tile) | Abbay project, drawn for Phase 7 | MIT | `app/src/main/res/drawable/` |
| `widget_*` vectors (home screen widgets) | Abbay project, drawn for Phase 7 | MIT | `app/src/main/res/drawable/` |
