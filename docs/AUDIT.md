# Public-edition release review

Review updated for 1.2.0 on 17 November 2026. This records engineering checks and licensing preparation, not a legal opinion or security certification.

## Distribution boundary
- Fresh Git repository, no private history copied.
- Only allowlisted source/build scripts and license records copied into the public tree.
- No personal album, photo metadata, server account/token, downloaded commercial wallpaper, soundtrack pack or Netflix startup recording.
- Three original procedural 4K loops and 19 original synthesized effects/welcome clips; generator and SHA-256 asset manifest included.
- Public APK has a distinct application identity and signing key. A private distribution retains its own identity and user media; code and fixes are shared through an edition profile.
- Upstream GPL attribution retained; Twemoji graphics CC BY 4.0, Cookie font OFL and dependency notices included in source and APK.
- No enabled publisher or premiere defaults. Optional web adapters are present in shared code but require source selection; provider terms and availability remain separate obligations.

## Local verification
| Check | Result |
|---|---|
| Debug + optimized release build | Passed |
| JVM regression/policy tests | 24 passed |
| Android instrumentation | 15 passed on isolated TV emulator |
| First-run setup | 8 pages, language switch, process restart/resume and saved completion passed |
| Display matrix | 20 screen/resolution cases passed: home, wallpapers, system, music, help across four configurations |
| Android lint | 0 errors; 98 warnings, including localization and deprecated/platform APIs |
| Public-content audit | Declared asset hashes and APK notice/media consistency checked |
| Release certificate | Dedicated RSA-4096 key, kept outside repository |
| Runtime dependency advisory check | 113 resolved coordinates checked with OSV; no findings after Guava 33.7.2 update (point-in-time check) |

GitHub Actions provides repeatable build/test reports and CodeQL separately checks Java/Kotlin. Current run results, rather than this document, are authoritative for later commits.

## Remaining limits
English covers the main UI, game labels and numeric templates; not every dynamic error or provider payload has been manually reviewed. Physical-device performance, vendor HDMI/CEC and Home handling are not certified by the emulator matrix. No exhaustive third-party-server integration or penetration test has been performed. Service availability and data completeness vary. Imported content requires its own rights; the project's licenses do not clear arbitrary media supplied later.

## Video changes in 1.2.0
Compressed video buffering is capped at 12 MiB with an eight-second time ceiling; codec output buffers are separate. Muted wallpapers do not decode audio/text tracks. The SurfaceView is rebound only when the player changes. One coroutine controls both transition phases and waits for the new video’s first frame. The two-minute timer is lifecycle-bound and stops behind menus/apps; no second decoder or periodic polling is used. These changes do not guarantee smooth playback of every imported codec/bitrate.

The real two-minute rotation was observed on an emulator and a physical TV, including rendering the next clip. A 129-second physical-device sample recorded 1,668 UI frames, 3.78% late frames and roughly 96 MiB PSS after rotation, with no Java crash reported. This sample includes a wallpaper transition; it is not a controlled before/after benchmark and does not establish that every source of stutter is fixed.
