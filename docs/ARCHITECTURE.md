# Architecture

NullBrowser is one Android application module. Its components are separated by responsibility even though v1 keeps the build graph intentionally small.

## Runtime components

### Main browser

`MainActivity` builds the native controls, configures WebView, interprets address-bar input, intercepts network requests, manages settings and statistics, handles downloads, and coordinates cleanup. Internal `https://harsha.search.local/...` routes are handled in-process and never require DNS.

### Search

`HarshaIndex` reads `assets/search/harsha_index.tsv` once and ranks matching records. `NullSearchProvider` generates the home and result documents. The web fallback is a normal link so the local-to-network boundary remains visible.

### Request privacy

`AdBlockerEngine` loads `assets/blocklists/trackers.txt` on a single-thread executor. WebView's request interceptor asks the engine about each host and returns an empty response for matches. A script injected after page load applies small canvas, hardware, and audio-surface changes; these are experiments, not a complete fingerprint defense.

### Local state

Small counters and mode preferences use `SharedPreferences`. Bookmarks use Room through `NullDatabase`. Cookies, cache, form data, WebStorage, and in-memory WebView history are cleared by the session controls. Downloads are handed to Android's Download Manager and therefore live outside the app's wipe boundary.

### Automation

`HeadlessAutomationService` owns a separate off-screen WebView. `LocalAgentServer` binds only to `127.0.0.1` and maps four HTTP routes to service methods. Calls currently block on `CompletableFuture.get()` and have no authentication, request limits, or structured error model. Keep this surface local and experimental.

### VPN scaffold

`PrivacyVpnService` requests an Android TUN interface, restricts it to the application package, and installs a default route. A useful VPN also needs a packet loop, tunnel protocol, relay/provider, failure handling, and leak testing; v1 does not provide those pieces.

### Native scaffold

The `cpp` folder contains a JNI proof of concept for key and signature checks. The current Android Gradle configuration does not make this module a security boundary, and secrets compiled into a client are extractable.

## Data boundaries

| Data | Storage | Cleared by app wipe? |
| --- | --- | --- |
| Cookies, cache, form data, WebStorage | Android WebView | Yes |
| Back/forward list | Current WebView instance | Yes |
| Mode preferences and counters | SharedPreferences | Yes during full wipe |
| Bookmarks | Room database | Yes during full wipe |
| Download log | SharedPreferences | Yes during full wipe |
| Downloaded files | Android public Downloads | No |
| Remote website logs | Remote servers | No |

## Extension points

- Search corpus: edit or generate the TSV asset without changing the UI.
- Blocking data: expand the domain asset or replace the matcher behind `shouldBlock`.
- Browser policy: keep WebView settings and intercept logic in `MainActivity` until they justify a dedicated policy class.
- Automation: add typed commands behind an opt-in, authenticated service before expanding the API.
- VPN: implement and test packet forwarding before presenting the surface as protection.
