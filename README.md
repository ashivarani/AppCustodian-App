# AppCustodian App

An Android utility app (Kotlin, Jetpack Compose, MVVM) for viewing installed
applications and streamlining the manual force-stop workflow through Accessibility
Service–assisted monitoring.

**Package:** `com.appcustodian.app`
**Minimum SDK:** 26
**Compile / Target SDK:** 36
**UI Toolkit:** Jetpack Compose, Material 3
**Version:** `0.1.0-Alpha01`

---

## Overview

Since Android 5.0, third-party applications cannot programmatically force-stop
other applications — `ActivityManager.forceStopPackage()` requires a
signature-level permission restricted to system and privileged (priv-app)
installs. This restriction applies uniformly across the Android ecosystem,
including all "task killer" applications distributed via the Play Store.

App Custodian does not attempt to bypass this restriction. Instead, it
streamlines the standard manual force-stop procedure and provides automated
feedback on its outcome, using an Accessibility Service in a strictly
observational capacity.

### Force-Stop Workflow

1. The user selects an application from the list and chooses **Force Stop**.
2. App Custodian opens the corresponding **Settings → App Info** screen.
3. The user manually taps **Force Stop** and confirms via the system dialog.
4. An Accessibility Service, enabled once by the user in system settings,
   monitors the App Info screen for changes in the Force Stop control's state.
5. Upon detecting a conclusive outcome, the service returns the user to App
   Custodian and reports the result via an in-app dialog and system
   notification, including an option to relaunch the application on success.

The Accessibility Service does not perform any click, tap, or button-press
actions. It is scoped exclusively to the system Settings package
(`android:packageNames="com.android.settings"`) and is armed only after an
explicit user-initiated force-stop request for a specific package.

### Outcome Detection Logic

`ForceStopAccessibilityService` arms itself with a target package immediately
before the Settings screen opens, then polls the active window at a fixed
500ms interval (following an initial settling period) for up to 60 seconds:

| Outcome              | Condition                                                                     |
|----------------------|-------------------------------------------------------------------------------|
| Success              | The Force Stop button or its confirmation dialog is no longer present.        |
| Failed — Disabled    | The button remains visible but disabled across two or more consecutive polls. |
| Failed — Not Present | No Force Stop control is found on an otherwise fully-loaded App Info screen.  |
| Timeout              | No conclusive state is reached within 60 seconds.                             |

In all cases, the service issues a system Back action (or relaunches App
Custodian if unavailable) and posts a corresponding result notification.

---

## Features

- **Application listing** — displays all installed applications, with an
  optional toggle to exclude system apps. Sorted by last-used time when Usage
  Access is granted; otherwise sorted alphabetically.
- **Search** — live filtering by application name or package identifier.
- **Force Stop / Open actions** — presented via a per-app action sheet. The
  Open action performs a clean relaunch using
  `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_CLEAR_TASK`;
  applications without a launcher activity are handled gracefully.
- **Permission prompting** — surfaces an Accessibility Service enablement
  prompt when required.
- **Result reporting** — in-app dialog and system notification summarizing
  the outcome of each force-stop attempt, with a failure reason where
  applicable.

---

## Known Limitations

- **OEM and locale variance.** The service matches Force Stop and confirmation
  controls first by AOSP resource ID (`com.android.settings:id/right_button`
  for API ~26–29, `com.android.settings:id/button1_positive` for API ~30+),
  falling back to English-language text matching ("Force stop", "OK"). OEM
  skins (Samsung One UI, MIUI, ColorOS) and non-English locales may require
  additional label or ID mappings in `ForceStopAccessibilityService.kt`.
- **Protected package filtering is heuristic.** A fixed set of known protected
  packages (`android`, `com.android.systemui`, `com.android.settings`,
  `com.android.phone`, `com.android.shell`) is excluded from the app list in
  `AppRepository.kt`. This list is not exhaustive; other protected or
  restricted applications may still appear and will report a disabled or
  not-present outcome.
- **Platform UI dependency.** Changes to the Settings app's UI across Android
  versions may affect node-matching accuracy until updated.
- **No live process enumeration.** Android has restricted visibility into
  other applications' running process state since Android 5.0. The
  application list reflects installed packages, optionally ordered by last
  usage, rather than a live process table.

---

## Permissions

| Permission                           | Purpose                                                                                     |
|--------------------------------------|---------------------------------------------------------------------------------------------|
| `QUERY_ALL_PACKAGES`                 | Enumerate all installed applications, not limited to those reachable via intent resolution. |
| `PACKAGE_USAGE_STATS` (optional)     | Sort applications by recent usage; falls back to alphabetical sorting if not granted.       |
| `POST_NOTIFICATIONS` (Android 13+)   | Deliver force-stop result notifications.                                                    |
| Accessibility Service (user-enabled) | Observe the Settings App Info screen to detect force-stop outcomes. No input is simulated.  |

---

## Building

This repository currently contains the `app/` module only; a root
`settings.gradle.kts` and Gradle wrapper are not yet included. Once integrated
into a complete project, open the module in Android Studio (Koala or later),
sync Gradle, and build via:

```
./gradlew assembleDebug
```

Release builds reference a `key.properties` file at the project root for
signing configuration. In its absence, `assembleRelease` produces an unsigned
APK and logs a warning; code shrinking and resource optimization
(`isMinifyEnabled`, `isShrinkResources`) are enabled for release builds
regardless of signing state.