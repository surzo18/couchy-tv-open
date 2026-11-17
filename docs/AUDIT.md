# Public-edition release review

Review performed for 1.0.0 on 16–17 November 2026. This records engineering checks and licensing preparation, not a legal opinion or security certification.

## Distribution boundary
- Fresh Git repository, no private history copied.
- Only allowlisted source/build scripts and license records copied into the public tree.
- No personal album, photo metadata, server account/token, downloaded commercial wallpaper, soundtrack pack or Netflix startup recording.
- Three original procedural 4K loops and 19 original synthesized effects/welcome clips; generator and SHA-256 asset manifest included.
- New application identity and signing key; no changes installed on the user's existing TV.
- Upstream GPL attribution retained; Twemoji graphics CC BY 4.0, Cookie font OFL and dependency notices included in source and APK.
- No bundled publisher feeds or HTML cinema scrapers. TVmaze/Open-Meteo attribution and service limitations documented.

## Local verification
| Check | Result |
|---|---|
| Debug + optimized release build | Passed |
| JVM regression/policy tests | 18 passed |
| Android instrumentation | 11 passed on isolated TV emulator |
| First-run setup | 8 pages, language switch, process restart/resume and saved completion passed |
| Display matrix | 20 screen/resolution cases passed: home, wallpapers, system, music, help across four configurations |
| Android lint | 0 errors; 97 warnings, including localization and deprecated/platform APIs |
| Public-content audit | Declared asset hashes and APK notice/media consistency checked |
| Release certificate | Dedicated RSA-4096 key, kept outside repository |

GitHub Actions provides repeatable build/test reports and CodeQL separately checks Java/Kotlin. Current run results, rather than this document, are authoritative for later commits.

## Remaining limits
English translation is incomplete. Physical-device performance, vendor HDMI/CEC and Home handling are not covered by the emulator matrix. No exhaustive third-party-server integration or penetration test has been performed. Service availability and data completeness vary. Imported content requires its own rights; the project's licenses do not clear arbitrary media supplied later.
