# App Custodian

Android app (Kotlin, Jetpack Compose, MVVM) that lists your installed apps in one
screen and lets you force-stop or reopen any of them with one tap.

Package: `com.appcustodian.app` · minSdk 26 · Compose + Material3

## How force-stop actually works here (read this first)

Since Android 5.0, **no normal app can silently force-stop another app** —
`ActivityManager.forceStopPackage()` requires a signature-level permission only
system/priv-app installs get. That applies to every app on the Play Store today,
including "task killer" apps.

This app uses the same technique those Play Store apps use:

1. It opens the target app's **Settings → App Info** screen for you.
2. An **Accessibility Service** (which you enable once, manually, in Settings)
   finds the real "Force stop" button on that screen and presses it — and
   presses "OK" on the confirmation dialog that follows.
3. The result (success/failure) is reported back to the app, which shows a
   popup and offers to reopen the app.

It is **not** a hidden API and **not** root — it's UI automation through a
permission you explicitly grant, scoped (via `accessibility_service_config.xml`)
to only look at the Settings app's screens.

### Known limitations (be aware before relying on this)

- **Button text varies by OEM/Android version.** `ForceStopAccessibilityService.kt`
  currently matches the stock/Pixel English strings ("Force stop", "OK"). Samsung
  OneUI, MIUI, ColorOS, and other locales may use different text or layouts —
  you'll need to extend `forceStopLabels` / `confirmLabels` and retest per device.
- **Protected system apps** (Settings itself, SystemUI, Phone, Shell, and a few
  others) have the Force Stop button greyed out or hidden by the OS — the app
  filters the most obvious ones out of the list (`PROTECTED_PACKAGES` in
  `AppRepository.kt`), but that list is a heuristic, not something Android
  exposes via API. If a button turns out to be disabled, the service reports a
  `Failed` result after an 8-second timeout rather than hanging.
- **Android version updates can change the Settings UI**, which may break the
  node-matching logic until the labels/hierarchy are updated.
- **"Running processes"**: Android also blocks apps from seeing other apps'
  live process list since Lollipop, so this app lists *installed* apps
  (optionally sorted by last-used time via Usage Access), not a live process
  table — there's no public API left for that.

## Permissions used

| Permission                                     | Why                                                                  |
|------------------------------------------------|----------------------------------------------------------------------|
| `QUERY_ALL_PACKAGES`                           | To list all installed apps, not just ones you've launched via intent |
| `PACKAGE_USAGE_STATS` (optional, user-granted) | To sort apps by "recently used" — declines gracefully if not granted |
| Accessibility Service (user-enabled)           | To press Force Stop / OK on the Settings screen                      |

## Build

Open the `AppCustodian/` folder in Android Studio (Koala+) and Sync Gradle, or:

```
./gradlew assembleDebug
```

On first launch, tap any app → **Force stop** → you'll be prompted once to turn
on the Accessibility Service. After that, force-stop requests happen with one
tap per app, no manual navigation needed.