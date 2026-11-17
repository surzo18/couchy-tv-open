# Security policy

Supported version: latest public 1.x release. This is a community project with
no guaranteed response time or enterprise support agreement.

Report vulnerabilities using the repository's **Security → Report a
vulnerability** feature. Do not put credentials, private server URLs or
exploitable details into a public issue. If private reporting is unavailable,
open an issue requesting a private contact without including sensitive details.

Include affected version, Android version, reproduction steps and impact.
Revoke exposed tokens on their source service. Never attach a signing key.

The release signing key is kept outside the repository. Debug-signed builds
are for development and are not official releases. Check the release checksum
and signing-certificate fingerprint in RELEASE.md before installing.

Home remapping and notification access are powerful optional permissions.
Only grant them to a build you trust. Keep the original launcher installed.
Use HTTPS for Jellyfin outside a trusted LAN. See PRIVACY.md for data flows.
