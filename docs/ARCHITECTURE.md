# Architecture

The Android app uses Kotlin, Jetpack Compose/TV Material and native Android dialogs where appropriate. Source namespace remains `com.conreo.couchytv` to preserve upstream structure; the public application ID is `io.github.surzo18.couchytv`.

| Source area | Responsibility |
|---|---|
| `ui/LauncherScreen.kt`, `MainActivity.kt` | Home surface, lifecycle and video background |
| `StudioActivity.kt`, `PortableSettings.kt` | On-device settings and library entry points |
| `WallpaperLibrary.kt`, `WallpaperCycle.kt`, `HomeMusic.kt` | Imported media, shuffle, audio lifecycle |
| `HomeNews.kt`, `NewsSources.kt` | User-configured feed acquisition and bounded caches |
| `Weather.kt`, `Premieres.kt` | Explicit weather location and documented TVmaze API adapter |
| `JellyfinLink.kt` | User-server connection, Quick Connect and browsing |
| `DiagnosticLog.kt`, `LegalDialogs.kt` | Local troubleshooting and bundled notices |
| `data/`, `ui/` | Persisted configuration, reusable UI and upstream app discovery |

Local preferences and app-private files hold configuration. Media imports use Android document selection; photo integration uses the platform media store. Media sessions and an optional notification listener provide external playback state. The optional accessibility service handles supported Home shortcuts. No cloud management backend or shared API credentials exist.

External services, request limits, attribution and commercial-use considerations are documented in [SERVICES.md](../SERVICES.md). Original media are reproducible with `scripts/generate-media.py`; SHA-256 provenance is in `media-manifest.json`. Release media/notice contents are checked against that manifest.

The project retains some legacy source names and manufacturer-specific fallbacks. Those do not imply every device supports the same hardware integrations. See [test scope](TESTING.md).
