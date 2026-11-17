# Shared code and distribution profiles

The launcher implementation is shared. A private distribution can keep its own
application ID, signing identity, installed data and licensed personal media
without maintaining a separate UI or network implementation.

`android/app/src/main/assets/edition.json` controls bundled wallpaper names and
collections, the wallpaper-cycle sentinel, album directories, welcome audio,
initial news presets and initial release-source selection. Saved user choices
take precedence over initial presets. Changing a language never changes stored
media IDs or filenames.

The public profile includes Aurora, Ocean and Ember, the original welcome sound,
empty news/release defaults and no personal photos or credentials. Optional
cinema/web adapters are present in common code but disabled until selected.
See [provider conditions](../SERVICES.md).

For a downstream distribution, copy the shared Kotlin, resources, tests and
manifest together. Keep its existing `applicationId`, signing key, edition
profile and private asset directories. Do not clear app data or uninstall as
part of an upgrade. Adding a new built-in wallpaper does not require replacing
the external media catalog. Keep personal media out of public Git history.

An in-place upgrade requires the same application ID and signing certificate.
The public APK is a separate app; installing it does not import another
distribution's account tokens, photos or preferences.
