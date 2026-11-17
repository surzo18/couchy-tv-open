# Contributing

This is the public Couchy TV Open edition, derived from Couchy Launcher.
Contributions to code are licensed GPL-3.0-only. Preserve upstream attribution.
Original demo media contributions need an explicit license and provenance;
copyrighted entertainment content and personal data are not accepted.

Use JDK 17+, Android SDK 34/build-tools 34.0.0 and Python 3.11+.
Set ANDROID_HOME, then run:

```sh
python scripts/audit-public.py
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

Use gradlew.bat on Windows. Integration tests require a dedicated Android TV
emulator. Never run reset/install automation against someone else's daily TV.
The package ID is io.github.surzo18.couchytv; minimum API is 29.

Keep PRs focused. Describe the problem, behavior and validation. For UI work,
attach a screenshot with original/demo media and test D-pad navigation, Back,
focus visibility and small layouts. Avoid continuous background polling, decode
work on the UI thread and unbounded caches. Preserve the original launcher.

Add regression tests for behavior changes. Do not claim broader device support
than you have tested. Slovak and partial English are supported; new user-facing
strings should be translated using the existing localization mechanism.

Be respectful and specific in reviews. Harassment, doxxing and discriminatory
conduct are not welcome. Maintainers may remove abusive content or restrict
participation. Report private concerns through the security contact route.
