# Privacy and permissions

Couchy TV Open has no developer-operated backend, advertising SDK, analytics
SDK, telemetry uploader or Couchy account. Settings and game progress are
stored on the TV. This does not mean optional online providers receive no data.

## Network activity

- News: only the HTTPS RSS/Atom URLs you add are queried. Publishers and image
  hosts receive your IP address and normal HTTP request metadata. Feeds refresh
  while the home interface is active, according to your selected interval.
- Weather: off by default. City search and forecasts contact Open-Meteo and
  disclose the query/selected coordinates and IP address. This edition does not
  automatically contact an IP geolocation service.
- New seasons: opening the panel contacts TVmaze for US schedule entries within
  three days before/after today. Posters are loaded from its image CDN.
- Jellyfin: only the server you configure is contacted. Its access token is
  stored in app-private preferences and excluded from cloud/device backups.
  Redirects are not followed for authenticated requests. Use HTTPS; HTTP sends
  credentials and metadata unencrypted and is only suitable for a trusted LAN.
- Opening an article, trailer, map or store link transfers control to another
  app and its privacy policy. The original provider may receive that request.

## Local information and optional permissions

- Installed-app visibility populates the launcher and app actions.
- Accessibility service, explicitly enabled in system settings, handles Home
  and foreground-app tracking for return-to-app. It is optional and does not
  upload this information.
- Notification access enables supported external-player metadata and controls.
  Track titles and artwork are displayed locally, not sent to the developer.
- Photo/video access provides albums and the screensaver. Imported files and
  edited captions remain on the device unless you explicitly export/share them.
- Network/location permission can expose the connected Wi-Fi name to the app;
  it is not used to scan GPS coordinates.

## Retention and removal

News, poster and artwork caches are bounded. Diagnostic records are app-local,
limited to 400 events/256 KiB and 30 days; a separate small last-crash record may
be retained for the next startup. Logs are not automatically uploaded. Review
any logs or screenshots before attaching them to a public issue.

Disconnecting Jellyfin removes its saved account token. Android's Clear data or
uninstall removes app-private settings and progress; MediaStore album files may
remain and can be managed through the TV's media/file tools. Android backup may
include other settings according to device policy. Backup exclusions explicitly
cover Jellyfin credentials. This app cannot revoke a token already copied by
another party; revoke it on your Jellyfin server if compromised.
