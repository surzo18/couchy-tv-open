# Online services and content policy

Reviewed 2026-11-16. This document distinguishes software licensing from access
to external services. Provider terms can change. No service availability or
third-party rights clearance is guaranteed by the application's GPL license.

## TVmaze

Only documented https://api.tvmaze.com endpoints are used. Data are licensed
under CC BY-SA 4.0: https://www.tvmaze.com/api#licensing . The panel credits
TVmaze, links to that license page and each show's source. Transformed or
redistributed TVmaze data remain subject to attribution and ShareAlike; this
does not impose CC BY-SA on the independent application code.

The application loads CDN image links at runtime as documented by TVmaze;
no movie/TV posters or API responses are shipped in the repository or APK.
Schedule coverage is explicitly US-oriented and limited to a seven-day window;
it does not promise a complete cinema or streaming catalog. Requests are bounded,
paced and cached for six hours. Publisher cinema HTML scrapers are excluded.

## Open-Meteo

https://open-meteo.com/en/terms
Data are CC BY 4.0; the free hosted API is restricted to non-commercial use and
subject to request limits. The application credits the provider and links to
its terms. It makes forecast calls only for the user's selected city, with a
30-minute refresh interval. City search uses Open-Meteo's geocoding API, whose
underlying location data are attributed to GeoNames (https://www.geonames.org/).

The GPL permits commercial use of the code. A commercial distributor must
separately arrange a suitable weather service plan/adaptor or disable this
integration; the code license does not waive service conditions. This release
ships the non-commercial public endpoint without a shared API key.

## User feeds and files

No news publisher feed is enabled or bundled by default. Add only feeds whose
terms permit your intended use. Headlines, summaries and images retain their
publishers' rights; an RSS URL is not a blanket redistribution license. There
is no developer-hosted aggregation proxy and no redistribution of downloaded
articles in this repo. The app opens original article URLs.

Import only media you are entitled to use. Public source, release and screenshot
checks allow only the project's procedural demo media and documented dependency
assets. Do not submit copyrighted soundtrack packs, TV recordings, scraped
poster collections, personal albums or credentials in contributions.

## Jellyfin and third-party applications

Jellyfin connects to a user-controlled server through Quick Connect; this does
not grant rights to the library's content. YouTube trailers and articles are
opened through external links, not downloaded or bundled. Third-party marks
identify integrations; this project is not affiliated with those providers.
