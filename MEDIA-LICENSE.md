# Original media and provenance

The public edition contains three original abstract animation loops (Aurora,
Ocean and Ember), their thumbnails, and 19 synthesized game/welcome sounds.
They are generated from mathematical expressions and oscillators in
`scripts/generate-media.py`. No footage, music recording, melody, film artwork,
anime character, sample pack or external image is used by that generator.

To the extent the project contributors hold rights in these generated media,
they dedicate them to the public domain under CC0-1.0. See
`licenses/CC0-1.0.txt`. The generator's code is GPL-3.0-only.

Reproduce with Python 3.11+ and FFmpeg (libx264):

    python scripts/generate-media.py

Encoding output may differ between FFmpeg versions. `media-manifest.json`
records the exact distributed bytes, source, license and checksum of each
included asset. Game character graphics are drawn by the GPL application code.
Screenshots show this application's UI with these original assets and demo data.

The couch icon is separate third-party artwork: Twemoji graphics, CC BY 4.0.
The Cookie typeface is SIL OFL 1.1. They are not covered by the CC0 dedication.
See THIRD-PARTY-NOTICES.md for the respective copyright notices.

No soundtrack collection, movie/anime wallpapers, startup brand sound,
personal album, account or viewing history is distributed. Users may import
media they are entitled to use. Importing a file does not grant rights to share
it with other people. References to third-party applications identify supported
integrations and do not imply sponsorship or endorsement.
