# Building and testing

Use JDK 17 or later, Android SDK platform 34/build tools 34.0.0, and Python 3.12+ for helper scripts. The wrapper pins Gradle 8.9 and validates its download checksum. No private media, accounts, signing key or service token is needed for debug builds.

```sh
python scripts/audit-public.py
cd android
bash gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
cd ..
python scripts/audit-public.py --apk android/app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Verify workflow runs JVM tests, lint, APK content checks and Android instrumentation on an isolated API 29 TV emulator. CodeQL separately analyzes Java/Kotlin, and Dependabot proposes dependency updates. Passing CI is not a penetration test or a promise of universal TV compatibility.

## Isolated device tests
Use a disposable TV emulator. Never run connected-device tests while a personal TV is connected to that Gradle environment. Install both debug/test APKs with an explicit emulator serial and run:

```sh
adb -s emulator-5556 shell am instrument -w io.github.surzo18.couchytv.test/androidx.test.runner.AndroidJUnitRunner
python scripts/test-onboarding.py --serial emulator-5556
python scripts/test-ui-matrix.py --serial emulator-5556
```

The onboarding script **clears this public app's data on the selected emulator**. Both scripts reject non-emulator serials. The matrix visits home, wallpapers, system, music and help at 720p, 1080p, UHD and compact/high-density settings, checks visible focus bounds and exercises navigation. It is a smoke test, not pixel-perfect comparison. Its screenshots/results are ignored by Git.

## Release checks
Audit the actual signed APK and inspect its certificate. Capture documentation images from this public build only. Confirm the media manifest, legal notices, no personal data/secrets, all relative documentation links and the release source tag. See [RELEASE.md](../RELEASE.md).

## Known validation limits
No representative physical-device performance minimum has been established. HDMI/CEC, vendor panels, system Home keys and third-party media sessions need real-device checks. The public edition is tested without a personal Jellyfin account; automated cases cover parsing/navigation policy, not every server or browser. Weather and schedule availability depend on external services. Localization tests cover static labels and numeric templates, including Android regex behavior; provider content and user metadata are not translated.

## Dependency maintenance

Version 1.2.0 retains the validated Gradle 8.9 / AGP 8.5.2 / Kotlin 2.0.20 / API 34 toolchain. Kotlin plugins are grouped so that compiler, Compose and serialization updates can be reviewed together. CI action updates are grouped, pinned to full commit hashes, and tested before merging. Security alerts remain enabled.

The release review incorporated graphics-shapes 1.1.0 and refreshed Python, artifact, emulator and CodeQL actions. Standalone proposals for AGP 9.5.1, Compose BOM 2026.09.00 and Kotlin Compose 2.4.21 failed CI: they respectively require newer Gradle, API 37, and migration to the compilerOptions DSL. Gradle 9.9.1 passed its individual checks but was deferred to a coordinated toolchain migration rather than changing the release baseline in isolation. These are deferred maintenance upgrades, not fixes required to install 1.2.0. Future updates must rerun the release checks for both editions.
