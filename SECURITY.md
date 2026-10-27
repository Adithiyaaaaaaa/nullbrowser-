# Security Policy

## Supported versions

Security fixes are applied to the latest tagged release and the `main` branch. Older experimental snapshots are not supported.

## Reporting a vulnerability

Please do not open a public issue for an exploitable vulnerability. Use GitHub's **Report a vulnerability** option in the repository Security tab when available. If private reporting is unavailable, contact the repository owner through the contact method on their GitHub profile and include only enough information to establish a secure follow-up channel.

Include the affected version, Android version, reproduction steps, impact, and any suggested mitigation. Do not include real browsing data, credentials, tokens, or third-party personal information.

## Scope reminders

NullBrowser v1 is experimental and has not received an independent security audit. Android WebView remains the rendering engine. The VPN and native-signature modules are scaffolds, the local automation server is unauthenticated, and Vault is a standard Room database rather than encrypted storage. These known limitations are documented in [docs/SECURITY.md](docs/SECURITY.md).
