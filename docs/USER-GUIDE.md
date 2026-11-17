# Using Couchy TV Open

## First launch
Complete the setup screens using the remote. Choose Slovak or English, review Home controls, and optionally add media, feeds, a city, Jellyfin and albums. You can skip optional pages. System → Run setup wizard reopens the flow.

## Remote controls
| Control | Action |
|---|---|
| Arrows / OK | Move focus / choose item |
| Back | Close panel or return |
| Hold OK on an app | Context menu and favorite management |
| Hold the game icon | Choose a minigame |
| Hold Back on home | Return to the most recent available app |
| Home / hold Home | Couchy / original home, when the optional accessibility service and TV support it |
| OK in a game | Pause menu |

Some browsers cannot restore the precise scroll position of a reopened article. Apps may decline media controls. Keep the system launcher enabled for recovery.

## Make it yours
The pencil opens settings. **Wallpapers** imports videos, manages shuffle and changes wake behavior. **Music** imports audio and assigns tracks to backgrounds; library management edits names/artist/origin and confirms permanent deletion. Video import is limited to 1.5 GB per file and preserves more than 2 GB free storage; audio import is limited to 100 MB per file. Only import media you may use.

**System → News sources** adds HTTPS RSS/Atom feeds and tests them before saving. A source needs valid publication dates and HTTPS article links. The public edition intentionally starts with an empty list. Weather is configured through its panel; choose a city explicitly, select a day for hourly detail and use Hide to remove its widget.

## Connect Jellyfin
Open Jellyfin → Account and connection, enter the reachable server URL and request a code. Authorize it in Quick Connect on the same server before it expires. A wrong-server or expired code will fail. Repeat in the panel after logout/expiry; there is no configuration-file editing. The separate Jellyfin TV app is needed for movie/episode playback. Prefer HTTPS; HTTP is only appropriate on a trusted local network.

## Photos and memories
Open Photo albums to import and organize media. Manage album selection for the screensaver, captions and individual inclusion. Home memories controls the optional pop-up's enabled state, frequency and duration. Select Couchy's photo screensaver in Android's system screensaver settings. No personal photos are supplied, and captions must be edited manually.

## Troubleshooting
1. Check Diagnostics and its event log for the affected source/media/network operation.
2. Confirm network access, the file's validity and required app permissions. A missing feed image or episode still may simply not be provided by the source.
3. For stuttering, try a static wallpaper or a lower-bitrate compatible video. Software-emulator timings do not establish your TV's playback capability.
4. If Home fails, open the original launcher/settings and check the accessibility service. Manufacturer updates can change key handling.
5. Report your Android version, steps and sanitized diagnostics through GitHub Issues. Never post account tokens, private server URLs, personal photos or passwords.

Full details: [feature reference](FEATURES.md). Data handling: [privacy](../PRIVACY.md). License/service information is also available offline under System → Licenses and privacy.

## New release sources
Open New releases and choose Sources. All are disabled initially. Select TVmaze API for a short series schedule, or optional cinema/web sources after reviewing their conditions. Source choices persist across restarts.

## Automatic wallpaper rotation
Automatic wallpaper changes are on by default: every two minutes on the cinematic home screen, the next eligible background fades through its preview using one video decoder. Turn this off in Wallpapers to keep your selection. Menus, other apps and the screensaver pause rotation; returning starts a fresh interval. The wake preference is independent. Assigned music follows the background.
