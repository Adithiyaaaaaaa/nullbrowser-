# Contributing to NullBrowser

Thanks for helping make NullBrowser clearer, safer, and more useful. The project favors small, auditable changes over large opaque rewrites.

## Good first contributions

- Add or correct entries in the local Harsha Search index.
- Add well-supported tracker hostnames to the blocklist.
- Write tests for ranking, domain matching, cleanup, or navigation.
- Improve accessibility labels, keyboard behavior, or small-screen layouts.
- Clarify documentation, security boundaries, or build instructions.

## Development setup

1. Fork and clone the repository.
2. Install Android Studio, Android SDK 35, and JDK 17 or newer.
3. Open the project and let Gradle sync.
4. Run `./build-apk.ps1` on Windows or `./gradlew assembleDebug` elsewhere.
5. Test on Android 8.0 / API 26 or newer.

## Pull request checklist

- Keep the change focused and explain the user-facing reason.
- Do not describe scaffolding as a completed security feature.
- Add or update tests when behavior changes.
- Run `assembleDebug` before submitting.
- Update the README or docs when module behavior or privacy boundaries change.
- Never commit signing keys, API secrets, browsing data, device logs, or personal URLs.

## Privacy and security changes

Privacy claims need evidence. In the pull request, describe the threat being addressed, the data flow before and after the change, remaining limits, and how reviewers can reproduce the result.

Do not open a public issue for a vulnerability that could put users at risk. Follow [SECURITY.md](SECURITY.md) instead.

## Commit style

Use a short, imperative subject such as `Add Harsha ranking tests` or `Document VPN forwarding limit`. Group unrelated changes into separate commits or pull requests.

By contributing, you agree that your contribution is licensed under the repository's GPL-2.0 license.
