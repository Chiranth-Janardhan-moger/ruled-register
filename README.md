# Register Book (RegiBook)

A digital ruled notebook and personal financial register for Android with English and Kannada language support. Includes dedicated modules for Pigmi daily collection ledger and LIC policy reminder cards.

## Features

- Pigmi Daily Collection Register: Account tracking, daily collection amounts, and address records.
- LIC Policy Reminder Cards: Policy number, policy tenure, premium amount, last payment, and next payment date reminders.
- Bilingual Support: English and Kannada locale switching.
- Notebook Theme: Ruled paper visual aesthetics with customizable red/black margin lines and light/dark theme support.
- In-App GitHub Auto-Updater: Automatic background release check against GitHub Releases on launch and manual update checking via Settings.
- Optimized Build: R8 minification and resource shrinking enabled for minimal APK size.

## Build and Run

### Prerequisites

- JDK 21 or later
- Android SDK (API 36, compileSdk 36, minSdk 24)

### Debug Build

```bash
./gradlew assembleDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

The signed release APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

### Run Unit Tests

```bash
./gradlew testDebugUnitTest
```

## Automated Releases via GitHub Actions

Publishing a new release tag triggers the automated build and release pipeline:

```bash
git tag v1.0.1
git push origin v1.0.1
```

The workflow will build the release APK with R8 enabled, sign the binary, and publish a new GitHub Release with the attached APK asset.
