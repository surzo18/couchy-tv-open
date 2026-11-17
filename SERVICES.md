# Online services and content policy

Reviewed 2026-11-16. This document distinguishes software licensing from access
to external services. Provider terms can change. No service availability or
third-party rights clearance is guaranteed by the application's GPL license.

## TVmaze

All release sources are disabled in the public edition until selected in **New releases → Sources**.

The optional TVmaze API adapter uses documented https://api.tvmaze.com endpoints.
API data are CC BY-SA 4.0: https://www.tvmaze.com/api#licensing . Attribution,
license and original-show links appear in the panel. Transformed/redistributed
data retain attribution and ShareAlike obligations; the independent app code
keeps its own license. The API adapter covers a US-oriented seven-day window,
with six-hour caching. It is not a complete streaming or cinema catalog.

## Optional web adapters

The common code also includes opt-in ITAFILM/CINEMAX cinema listings and a
TVmaze web-calendar adapter. These are independent web integrations, not
official publisher APIs. Their inclusion does not grant permission to access,
extract or redistribute a provider's content. Check the terms of
https://www.itafilm.sk/ , https://www.cine-max.sk/ and https://www.tvmaze.com/
before enabling them for your intended use; leave them disabled if permission
is unclear. A confirmation in the app is not a substitute for those rights.

Web layout changes can break adapters. Requests, cache size and result counts
are bounded. Article/show links identify the original sources. No posters,
publisher article snapshots or scraped responses ship in this repository or
APK. Images are retrieved at runtime only for enabled features. A downstream
private distribution can supply its own presets through its edition profile;
such presets do not become defaults in this public distribution.

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
