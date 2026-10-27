# 🛡️ App Custodian – Android App Management Utility

## 📌 Project Description

**App Custodian** is an Android utility application developed using **Kotlin**, **Jetpack Compose**, and **Material 3**. It helps users browse installed applications, search and filter them, relaunch apps, and carry out Android's **manual force-stop workflow** with Accessibility Service–assisted status monitoring.

Android does not allow ordinary third-party applications to force-stop other apps directly. App Custodian respects this restriction: it opens the selected application's **system App Info screen**, where the **user must tap Force stop and confirm**. Its Accessibility Service observes relevant changes in the Settings interface and attempts to report the result.

## ✨ Features

- 📱 **Installed App List:** View installed applications with their names, icons, and package identifiers.
- 🔎 **Live Search:** Filter applications by name or package name.
- ⚙️ **System App Filter:** Show or hide system applications.
- 🕒 **Recent Usage Sorting:** Sort apps by their last-used time when Usage Access is granted; otherwise, list them alphabetically.
- 🛑 **Manual Force-Stop Assistance:** Open the system App Info page for a selected app to perform a manual force stop.
- ♿ **Accessibility-Based Monitoring:** Observe the force-stop control in Android Settings after a user-initiated request.
- ✅ **Outcome Feedback:** Show an in-app result dialog and, when permitted, a notification for detected success or failure.
- 🚀 **App Relaunch:** Open an installed app that has a launcher activity.
- 🎨 **Modern Interface:** Use Jetpack Compose and Material 3 for the app list, search, dialogs, and actions.

**Important:** The Accessibility Service does **not** press the Force stop or confirmation buttons for the user. Status detection is based on Settings UI observations and may not be reliable on every Android device.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Kotlin | Android application logic |
| Android Studio | Development environment |
| Jetpack Compose | Declarative user interface |
| Material 3 | UI components and theme |
| MVVM / AndroidViewModel | UI state and application logic |
| Kotlin Coroutines & StateFlow | Asynchronous data loading and reactive UI state |
| Android PackageManager | Installed-app discovery and app launch intents |
| UsageStatsManager | Optional last-used timestamps |
| Accessibility Service | Observe the system Settings force-stop workflow |
| Android Notifications | Report detected force-stop outcomes |
| Gradle (Kotlin DSL) | Build configuration |

## 📂 Project Structure

```text
AppCustodian-App-master/
├── Apk/
│   └── App_Custodian.apk
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/appcustodian/app/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── model/AppInfo.kt
│       │   │   └── repository/AppRepository.kt
│       │   ├── service/ForceStopAccessibilityService.kt
│       │   ├── ui/
│       │   │   ├── AppViewModel.kt
│       │   │   ├── screens/AppScreen.kt
│       │   │   ├── components/
│       │   │   │   ├── AppActionSheet.kt
│       │   │   │   └── AppListItem.kt
│       │   │   └── theme/
│       │   └── util/
│       │       ├── ForceStopBridge.kt
│       │       ├── NotificationHelper.kt
│       │       └── PermissionUtils.kt
│       └── res/
│           ├── drawable/
│           ├── values/
│           └── xml/accessibility_service_config.xml
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Main Components

- **`MainActivity.kt`** – Starts the Compose UI, handles notification permission, and opens Android Settings or other apps.
- **`AppRepository.kt`** – Retrieves, filters, and sorts installed app information.
- **`AppViewModel.kt`** – Manages search, filtering, app selection, and force-stop result state.
- **`AppScreen.kt`** – Displays the searchable app list and associated dialogs.
- **`AppActionSheet.kt`** – Provides Force Stop and Relaunch options.
- **`ForceStopAccessibilityService.kt`** – Monitors the system Settings UI during the manual force-stop workflow.
- **`ForceStopBridge.kt`** – Passes the selected package and detected results between the UI and service.
- **`PermissionUtils.kt`** – Checks permission states and creates system Settings intents.
- **`NotificationHelper.kt`** – Creates notification channels and posts outcome notifications when allowed.

## 🚀 Installation and Setup

### Prerequisites

- Android Studio with the required Android SDK and Gradle tooling
- JDK compatible with the project's Android Gradle Plugin
- Android device or emulator running **Android 8.0 (API 26)** or later

### Step 1: Open the Project

Download or clone the repository, extract it if necessary, and open the project folder in **Android Studio**.

### Step 2: Sync and Build

Allow Gradle to download dependencies and finish synchronization. Then select **Run** in Android Studio, or build a debug APK from the project root:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK is normally found at `app/build/outputs/apk/debug/app-debug.apk`.

### Step 3: Enable Accessibility Service

1. Launch **App Custodian**.
2. Follow the app's prompt to open Android **Accessibility** settings.
3. Find **AppCustodian App** and enable its Accessibility Service after reviewing the system disclosure.
4. Return to the app to use the assisted force-stop workflow.

For notifications on Android 13+, grant notification permission when prompted. **Usage Access is optional** and must be enabled separately in the device's settings if you want recent-usage sorting.

## 📱 How to Use

1. Launch App Custodian to view installed applications.
2. Use the search bar to find an app by name or package identifier.
3. Use the **Show system / Hide system** filter to control whether system applications appear.
4. Tap an app to open its actions.
5. Choose **Relaunch** to open it, if the application has a launcher activity.
6. Choose **Force Stop** to open that application's Android **App Info** screen.
7. **Manually** tap **Force stop** and confirm in the system dialog.
8. Review the detected result in App Custodian or its notification, when available.

## ⚙️ How the Force-Stop Workflow Works

1. **User selection:** The app saves the chosen package name and opens the corresponding system App Info screen.
2. **UI observation:** Once enabled, the Accessibility Service observes the Settings app's interface. It does not tap the Force stop button or confirmation control.
3. **Status detection:** The service checks the control's presence and enabled state, polling at roughly **500 ms** intervals after an initial settling period.
4. **Result reporting:** It reports a detected success or a failure reason (such as a disabled or missing control). The monitoring attempt times out after approximately **60 seconds**.
5. **Navigation:** The service attempts to return to App Custodian and displays the result through the app interface and notifications.

This is a **best-effort, UI-based detection method**, not a guarantee that Android has successfully stopped a process.

## 🔐 Permissions

| Permission / Access | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | List installed applications on the device |
| `PACKAGE_USAGE_STATS` (optional special access) | Read recent usage timestamps for sorting |
| `POST_NOTIFICATIONS` (Android 13+) | Display force-stop outcome notifications |
| Accessibility Service (user-enabled) | Observe the Android Settings interface during a requested force-stop operation |

The Accessibility Service is configured to receive accessibility events from **`com.android.settings`**. It requires explicit user enablement and can be disabled at any time in system Settings.

## 📌 Project Information

| Detail | Value |
|---|---|
| Project Name | App Custodian |
| Application Type | Android Utility |
| Language | Kotlin |
| UI Toolkit | Jetpack Compose, Material 3 |
| Architecture | MVVM-style state management |
| Package Name | `com.appcustodian.app` |
| Minimum SDK | API 26 (Android 8.0) |
| Target SDK | API 36 |
| Compile SDK | API 36 |
| Version | `0.1.0-Alpha01` |

## ⚠️ Current Limitations

- Android restricts direct force-stopping by third-party apps. **Users must perform the action themselves in system Settings.**
- Force-stop outcome detection depends on Settings screen elements and can vary by Android version, device manufacturer, and language. Results can be inaccurate.
- Some protected system apps cannot be force-stopped; a small set of known protected packages is excluded from the displayed list.
- This is **not** a live running-process manager. It lists installed applications, not all active processes.
- Usage-based sorting requires manually granted Usage Access; without it, the app sorts alphabetically.
- Apps without a launcher activity cannot be reopened using the Relaunch option.
- `QUERY_ALL_PACKAGES` and Accessibility Service use may require additional review for app-store distribution.

## 📦 APK and Build Notes

The repository includes an APK at `Apk/App_Custodian.apk`. A fresh debug APK can also be built from source with Gradle.

Release signing is configured through an optional root-level `key.properties` file. If that file is absent, the release build is **unsigned**. Release builds also enable code and resource shrinking.

## 🎯 Learning Outcomes

This project demonstrates:

- Building Android apps using Kotlin and Jetpack Compose.
- Working with PackageManager and installed application metadata.
- Implementing live search, filters, and lazy lists.
- Managing asynchronous application state with ViewModel, Coroutines, and StateFlow.
- Integrating Android Accessibility Services while respecting platform restrictions.
- Handling special access, runtime permissions, and system Settings intents.
- Sending Android notifications and handling application navigation.

## 📝 Conclusion

**App Custodian** is an Android utility project focused on installed-app discovery and assisting users with Android's manual force-stop procedure. It combines a Compose-based interface, application management actions, and Accessibility Service monitoring to demonstrate practical Android platform integration without claiming privileged force-stop capabilities.
