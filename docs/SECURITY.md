# Security and privacy model

## What v1 tries to protect

NullBrowser reduces app-side browsing traces, blocks a small set of known tracker domains, makes JavaScript and images easy to disable, and gives the user an immediate local wipe control. Secure-screen and biometric features reduce casual exposure when a device is shared or an app is backgrounded.

## What v1 does not protect

- Websites, networks, DNS resolvers, search providers, and Android may still observe traffic or metadata.
- WebView inherits Chromium/WebView vulnerabilities and fingerprint surfaces from the installed Android System WebView.
- `FLAG_SECURE` discourages normal screenshots; it does not defeat a compromised operating system or an external camera.
- Root detection and debugger checks are signals, not reliable compromise detection.
- JavaScript fingerprint modifications are partial and can make a browser more unusual.
- Files saved through Android Download Manager remain after an app wipe.
- Vault is a normal Room database in v1; its name does not imply encryption.
- The VPN service creates a TUN interface but has no forwarding tunnel or provider backend.
- The loopback agent server has no authentication and should not be exposed or port-forwarded on an untrusted machine.

## Threat-boundary checklist

| Scenario | v1 position |
| --- | --- |
| Someone casually opens the app after it was backgrounded | Biometric timeout can help |
| Someone takes a normal Android screenshot | Secure-screen flag blocks it in production UI |
| A known tracker hostname appears as a subresource | Domain matcher can block it |
| A site uses an unlisted hostname or first-party tracking | Not reliably blocked |
| Device is rooted or OS is compromised | Warning may appear; confidentiality is not guaranteed |
| User performs a full wipe | App-managed WebView state, preferences, and bookmarks are cleared |
| A downloaded file contains sensitive content | Outside the wipe boundary |
| User expects VPN anonymity | Not provided in v1 |

## Responsible claims

When contributing, describe observed behavior rather than absolute guarantees. Prefer “clears app-managed WebView state” to “leaves no trace,” “blocks listed tracker domains” to “blocks all ads,” and “VPN scaffold” to “VPN protection.”

## Recommended development posture

Use synthetic test data, a disposable emulator profile, and non-sensitive websites. Do not forward the local agent port on shared or untrusted networks. Review WebView and Android security updates before publishing a release.
