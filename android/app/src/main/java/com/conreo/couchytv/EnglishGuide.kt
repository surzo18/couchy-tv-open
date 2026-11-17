package com.conreo.couchytv

internal val englishGuide=listOf(
    HelpTopic("Getting started","Couchy is a home screen for Android TV and Google TV. Use the directional pad, OK and Back.",listOf("Home → Couchy","Hold Home → original launcher"),listOf(
        "Home shortcut" to "Open Home settings → System and enable the accessibility shortcut. Some TV manufacturers reserve Home and do not allow remapping. The status shown here tells you whether the service is enabled.",
        "Return to an app" to "Hold Back on the home screen to resume the last app. Short Back closes the current panel. In supported lists, Right moves directly to Close.",
        "Language and display" to "Choose Slovak or English in System. The TV's system language stays unchanged. Layout scales to the available screen; picture quality still depends on the TV's video decoder.")),
    HelpTopic("Wallpapers and music","Import media, edit its metadata and choose how the home screen looks and sounds.",listOf("Wallpapers → Add video","Music → Add tracks"),listOf(
        "Wallpapers" to "Choose a wallpaper and open Manage wallpaper to rename it, change its category, assign music or delete an imported file. Built-in wallpapers can be disabled but cannot be deleted. At least one wallpaper must remain enabled.",
        "Music" to "Add audio files through the file picker. Each file is limited to 100 MB. Manage tracks lets you change the title, artist, origin, wallpaper bindings and inclusion in playback. Permanent deletion requires confirmation.",
        "Playback order" to "Tracks assigned to a wallpaper play in shuffled order. After the theme queue finishes, music continues randomly without immediate repeats. Configure the fallback for wallpapers without music, volume and startup sound in Music.",
        "Wake behavior" to "Choose random wallpaper or keep the last wallpaper on wake. Your choices persist after reboot and app updates; clearing app data removes settings.")),
    HelpTopic("News","Read news on the ticker or open the list to see summaries and pictures.",listOf("News → Sources","System → News refresh"),listOf(
        "Manage sources" to "Add an HTTPS RSS or Atom URL, name, category and #RRGGBB color. Test it before saving. You can disable, edit or remove any source, or clear the configured sources. Up to 20 feed entries are supported.",
        "Filtering and refresh" to "Scroll the source selector horizontally and choose a source. Refresh intervals are 5, 15, 30 or 60 minutes. Articles from the last 48 hours are included. Failed requests keep eligible cached results, with details in Diagnostics.",
        "Bounded memory" to "The cache is limited to 180 articles. Feed responses, images and image caches have size limits. Disabling the ticker stops its periodic refresh. Custom feeds must provide publication dates and usable HTTPS article links.")),
    HelpTopic("Weather","Open the weather icon to select a location and see daily and hourly forecasts.",listOf("Weather → Choose city","°C / km/h ↔ °F / mph"),listOf(
        "Location" to "Search for a city and select a result. The selection is saved. No IP-based location lookup is performed. Open-Meteo and GeoNames provide location data.",
        "Forecast" to "Choose a day for its hourly forecast. The source is Open-Meteo; forecasts refresh every 30 minutes while the home screen is active. You can hide the weather widget and change temperature and wind units.",
        "Providers" to "The weather provider is an integrated API, not an arbitrary RSS address. Changing it requires an adapter for its data format.")),
    HelpTopic("Jellyfin","Connect your own server using Quick Connect. Credentials are stored only on the device.",listOf("Jellyfin → Account and connection","Continue watching · Next episodes"),listOf(
        "Connect" to "Enter your Jellyfin server URL and request a code. Authorize that code on the same server. If your session expires, reconnect in this panel; editing configuration files is not necessary.",
        "Browse" to "Continue watching and Next episodes provide shortcuts. What to watch offers a random selection, including favorites. Open a series to choose a season and episode. Availability depends on your server and permissions.",
        "Artwork" to "Episode and landscape artwork is preferred where available. Missing artwork uses a fallback. Media descriptions and titles come from your server and are not translated by the launcher.")),
    HelpTopic("Photos and memories","Personal photos are not included in this package. Use Photo albums to manage your own media.",listOf("Photo albums → Albums and screensaver","Home memories → timing"),listOf(
        "Albums" to "The photo library reads the Pictures/Couchy-Album and Pictures/Couchy-Hidden folders on the TV. Select albums for the screensaver and edit captions and album names. Android may request permission to modify media.",
        "Memory widget" to "Enable or disable home memories and choose their interval and display duration. Eligible photos rotate without repeating the same photo until the pool is exhausted. Open a memory for full-screen viewing.",
        "Screensaver" to "Select the photo screensaver in the TV's system settings. Arrows change photos; OK or Back exits. Album labels, date, clock and optional weather are drawn with readable backgrounds.")),
    HelpTopic("Apps, HDMI and playback","Customize favorites and use the media controls for supported apps.",listOf("Hold OK → manage apps","HDMI names → automatic or custom"),listOf(
        "Apps" to "Choose favorites and reorder them. Apps removed from favorites remain available in All apps. Availability and app launch behavior depend on the TV.",
        "HDMI" to "Only connected inputs are shown. Names come from the TV's input service and HDMI-CEC where available. Override a name if needed; removing the override restores automatic naming. External Android TV boxes may not expose TV HDMI inputs.",
        "Now playing" to "Grant notification access for playback controls and track information from apps such as Spotify. Some players expose limited metadata or do not allow remote controls. Wallpaper music yields to other playback.")),
    HelpTopic("Games","Hold the game icon to choose a game. Progress is stored locally.",listOf("Cross the road","Scooter"),listOf(
        "Cross the road" to "Use the directional pad to cross roads and rivers. Difficulty grows with progress. Coins unlock characters. OK opens the pause menu; the game stops advancing while paused.",
        "Scooter" to "Ride automatically, press Up to jump and Down to duck. Left and Right control speed. Collect coins, complete challenges and unlock items.",
        "Sound and progress" to "Game sound effects are available when music is not playing. High scores, unlocks and daily challenges are saved. Clearing app data removes progress.")),
    HelpTopic("Diagnostics","Use this screen to inspect failures and the current state of the app.",listOf("Diagnostics → event log","Settings → backup"),listOf(
        "Logs" to "The app records bounded diagnostic events. Entries expire after 30 days and are capped at 400 items and 256 KB. They do not intentionally store credentials, article titles or personal photo metadata.",
        "Compatibility" to "This package requires Android TV / Google TV on Android 10 or later. Samsung Tizen and LG webOS cannot install Android APK files. Home shortcuts, HDMI-CEC, picture settings and 4K decoding vary by hardware.",
        "Recovery" to "Keep the original system launcher enabled. If Home remapping stops, check the accessibility service in System. Imported media and settings survive updates signed with the same key."))
)
