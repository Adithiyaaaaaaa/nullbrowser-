# NullBrowser

I am working on a browser that's null: minimal, private-by-default, and intentionally small.

NullBrowser is a privacy-first Android browser prototype built with native Android `WebView`. It is intentionally small: one Android app module, Java source, no third-party app dependencies, and a direct build path to a debug APK.

The project started as an experiment in building a browser that behaves more like an always-private session than a normal mobile browser. It does not try to replace Chromium or build a browser engine from scratch. Instead, it uses Android's system WebView and hardens the app around it.

## Pictures

Real Android screenshots are intentionally blocked by `FLAG_SECURE`, which is part of the app's privacy behavior. The images below are repository mockups/feature visuals for README and project presentation.

![NullBrowser browser UI mockup](docs/images/browser-ui-mockup.svg)

![NullBrowser privacy features](docs/images/privacy-features.svg)

## Project status

Current version: `1.0.0`

Current package name:

```text
com.nullbrowser.privacy
```

## Version 1.0.0: Hybrid Privacy & Automation Engine

NullBrowser has evolved into a hybrid engine that supports both secure human browsing and on-device headless automation for AI agents.

### New Features in 1.0.0

- **Agentic Headless Mode:** An ultra-lightweight execution environment that bypasses rendering to provide semantic Markdown for LLMs.
- **Local Agent Server:** A localhost-bound (127.0.0.1) server for driving the browser via a simplified CDP/MCP protocol.
- **Biometric Session Lock:** 30-second background timeout protection with Biometric/PIN unlock and 5-attempt panic wipe.
- **VPN Kill Switch:** Strict traffic enforcement that drops all packets if the privacy tunnel is compromised.
- **Native Security Layer:** JNI-based signature verification and anti-tamper checks.
- **Anti-Fingerprinting:** Real-time JS injection to spoof Canvas, Audio, and Hardware fingerprints.
- **Ad/Tracker Engine:** High-performance domain-matching engine.
- **Modern Build System:** Fully converted to Kotlin DSL and Gradle 8.13.

### Hybrid Architecture

- **MainActivity (Human Mode):** Secure WebView wrapper with hardened privacy settings.
- **HeadlessAutomationService (Agent Mode):** Background service for headless DOM processing.
- **LocalAgentServer:** WebSocket/HTTP interface for AI automation.
- **PrivacyVpnService:** Now includes a hardware-level Kill Switch.

## Build requirements

Known working setup:

- Windows.
- Android Studio installed.
- Android Studio bundled JBR.
- Android SDK Platform 35.
- Android SDK Build-Tools 35.
- Gradle wrapper 8.13.
- Android Gradle Plugin 8.11.1.

The project includes:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
build-apk.ps1
```

`build-apk.ps1` sets `JAVA_HOME` to Android Studio's bundled Java runtime and then calls Gradle.

## Build the APK

From the project root:

```powershell
.\build-apk.ps1
```

That runs:

```powershell
.\gradlew.bat assembleDebug
```

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Install on a connected Android phone

Enable Developer Options on the phone:

1. Open Android Settings.
2. Go to About phone.
3. Tap Build number seven times.
4. Open Developer options.
5. Enable USB debugging.
6. Connect the phone with USB.
7. Accept the "Allow USB debugging?" prompt.

Then run:

```powershell
.\build-apk.ps1 installDebug
```

If the device says `UNAUTHORIZED`, unlock the phone and approve the USB debugging prompt. If no prompt appears, revoke USB debugging authorizations, unplug/replug USB, and try again.

## Launch from ADB

If `adb.exe` is available:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.nullbrowser.privacy/.MainActivity
```

Check whether the app process is running:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pidof com.nullbrowser.privacy
```

Inspect recent logs:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -d -t 200
```

## Run from Android Studio

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Connect a phone or start an emulator.
4. Choose the `app` run configuration.
5. Press Run.

## Development workflow

Useful commands:

```powershell
.\build-apk.ps1
.\build-apk.ps1 installDebug
.\build-apk.ps1 clean
```

For a quick source search:

```powershell
rg "panicWipe|clearSessionData|prepareVpn|normalizeAddress" app/src/main
```

## Security roadmap

High-priority next steps:

- Make the app fully blank-by-default instead of opening DuckDuckGo on launch.
- Add a setting for search provider or disable search fallback entirely.
- Add PIN unlock.
- Add biometric unlock.
- Add automatic wipe after failed unlock attempts.
- Add a true foreground VPN tunnel.
- Add a VPN kill switch.
- Add stronger certificate and backend pinning once a VPN/proxy backend exists.
- Add release signing and signature self-checks.
- Add stricter anti-debug and anti-tamper checks for release builds.
- Add tracker and fingerprinting blocklists.
- Add download handling with private cleanup rules.
- Add automated tests for cleanup behavior.

## Release roadmap

Before publishing or sharing widely:

- Create a release keystore.
- Build a signed release APK or AAB.
- Remove debug-only assumptions.
- Audit WebView settings.
- Add a privacy policy.
- Add a clear threat model.
- Test on multiple Android versions.
- Test cold start, rotation, process death, and low-memory behavior.
- Test USB install, direct APK install, and Android Studio install.

## Threat model notes

NullBrowser is trying to reduce local browsing traces and make privacy defaults harder to forget.

It does not currently defend against:

- A compromised Android OS.
- A malicious keyboard.
- A malicious VPN server.
- Browser engine vulnerabilities in Android WebView.
- Advanced website fingerprinting.
- Cellular provider metadata collection.
- Physical device compromise while unlocked.

These are solvable only partially, and each requires a more explicit security design.

## Git hygiene

The repository ignores:

- Build output.
- APK/AAB artifacts.
- `.gradle`.
- `.idea`.
- `local.properties`.
- OS noise files.

Debug APKs should be rebuilt locally instead of committed.

## License

Adithiyaaaaaaa/nullbrowser- is licensed under the
GNU General Public License v2.0

check more on lICENSE.md

