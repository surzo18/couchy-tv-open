# Releases and verification

Public application ID: `io.github.surzo18.couchytv`.

## Verify a download
Each release provides a signed APK, `SHA256SUMS.txt`, a runtime dependency report and its exact source tag/archive. Compare the downloaded APK hash with the published checksum. To verify the Android signing certificate:

```sh
apksigner verify --print-certs Couchy-TV-Open-1.0.0.apk
```

Expected signer SHA-256:
```
357a2bfbf18a4a9c2df97e0c4dc233d23888698dcfb8c8b11150abe73edb7780
```

Checksums detect unexpected changes; the certificate identifies updates signed with the release key. Do not install a debug build over a release signed with another key.

## Maintainer procedure
1. Review media provenance, credentials/personal data, service conditions and dependency notices. Sync bundled notices if changing root legal documents.
2. Pass `scripts/audit-public.py`, build/lint/JVM/instrumentation tests and inspect representative TV screens.
3. Set `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in a private environment. Never commit keys, passwords or a personal Android backup. Run `:app:assembleRelease`.
4. Audit the signed APK with `scripts/audit-public.py --apk PATH`. Verify its certificate and write SHA-256 checksums.
5. Publish the APK only together with its corresponding source tag. Retain complete source/build scripts and referenced dependency versions as required by GPL distribution. GitHub's source archives identify that tag; runtime dependencies are also reported with the release.
6. Keep the release signing key backed up securely outside Git. A lost key prevents ordinary in-place updates. A local `-PlocalSign` build is for testing only and must not be presented as an official release.

Release creation is manual; CI never receives the private signing key. See [test scope](docs/TESTING.md), [review findings](docs/AUDIT.md) and [licensing](LICENSING.md).
