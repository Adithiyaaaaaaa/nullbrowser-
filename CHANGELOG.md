# Changelog

All notable project changes are recorded here. The project follows Semantic Versioning where practical.

## [1.0.0] - 2026-10-28

### Added

- Native mobile-first Android browser shell with bottom navigation.
- Bundled Harsha Search index with local ranking and an explicit web fallback.
- Asset-backed tracker-domain interception and best-effort fingerprint defenses.
- Strict JavaScript and image-blocking modes.
- Session cleanup, manual panic wipe, ten-day inactivity wipe, and biometric timeout.
- Room-backed bookmark Vault.
- Download confirmation, local statistics, policy, and page-information surfaces.
- Experimental headless WebView service and loopback agent API.
- Android VPN and JNI signature-verification scaffolds for future work.
- Community documentation, CI, contribution guidance, issue templates, and an honest security model.

### Known limitations

- The VPN scaffold does not forward packets to a privacy relay or remote server.
- Vault storage is not encrypted at rest.
- The local agent API is unauthenticated and starts with the main activity.
- Anti-fingerprinting changes are incomplete and may increase uniqueness.
- The bundled search index and tracker list are intentionally small.

[1.0.0]: https://github.com/Adithiyaaaaaaa/nullbrowser-/releases/tag/v1.0.0
