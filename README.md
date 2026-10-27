# 🛡️ App Custodian – Android App Management Utility

## 📌 Project Description

**App Custodian** is an Android utility application developed using **Kotlin**, **Jetpack Compose**, and **Material 3**. It helps users view installed applications, search and filter app records, launch apps, and follow Android's manual **Force Stop** workflow with **Accessibility Service–assisted outcome monitoring**.

Android restricts ordinary third-party applications from directly force-stopping other apps. App Custodian does not bypass this restriction. Instead, it opens the selected application's **system App Info screen**, where the user manually performs and confirms the force-stop action.

The application's Accessibility Service observes relevant changes in the Android Settings interface and attempts to report the outcome through in-app dialogs and notifications.

This project demonstrates Android application management, MVVM-style architecture, package information retrieval, Accessibility Service integration, and system permission handling.

## ✨ Features

- 📱 **Installed App Listing:** Displays installed applications with their names, icons, and package identifiers.
- 🔎 **Live Search:** Search applications by name or package identifier.
- ⚙️ **System App Filtering:** Show or hide system applications from the list.
- 🕒 **Recent Usage Sorting:** Sort applications based on their last-used time when Usage Access is granted, with alphabetical sorting as a fallback.
- 🛑 **Manual Force-Stop Assistance:** Opens the selected application's system App Info screen for manual force stopping.
- ♿ **Accessibility Service Monitoring:** Observes the Android Settings interface during a user-initiated force-stop workflow.
- ✅ **Outcome Feedback:** Displays the detected operation result using in-app dialogs and notifications.
- 🚀 **App Relaunch:** Launches selected applications when they have an available launcher activity.
- 🎨 **Modern UI:** Provides an interactive interface using Jetpack Compose and Material 3.
- 🔐 **Permission Management:** Handles Accessibility Service access, Usage Access, and notification permissions.

**Note:** The Accessibility Service does not automatically click the Force Stop or confirmation buttons. These actions must be performed manually by the user.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Android Studio | Development environment |
| Jetpack Compose | User interface development |
| Material 3 | UI components and styling |
| MVVM Architecture | Application logic and UI state management |
| AndroidViewModel | Lifecycle-aware state management |
| Kotlin Coroutines | Asynchronous operations |
| StateFlow | Reactive UI state updates |
| PackageManager | Installed application information |
| UsageStatsManager | Recent application usage information |
| Accessibility Service | System Settings UI monitoring |
| Android Notifications | Force-stop result notifications |
| Gradle (Kotlin DSL) | Build configuration and dependency management |

## 📂 Project Structure

```text
AppCustodian-App-master/
│
├── Apk/
│   └── App_Custodian.apk
│
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   │
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           │
│           ├── java/com/appcustodian/app/
│           │   ├── MainActivity.kt
│           │   │
│           │   ├── data/
│           │   │   ├── model/
│           │   │   │   └── AppInfo.kt
│           │   │   └── repository/
│           │   │       └── AppRepository.kt
│           │   │
│           │   ├── service/
│           │   │   └── ForceStopAccessibilityService.kt
│           │   │
│           │   ├── ui/
│           │   │   ├── AppViewModel.kt
│           │   │   ├── screens/
│           │   │   │   └── AppScreen.kt
│           │   │   ├── components/
│           │   │   │   ├── AppActionSheet.kt
│           │   │   │   └── AppListItem.kt
│           │   │   └── theme/
│           │   │
│           │   └── util/
│           │       ├── ForceStopBridge.kt
│           │       ├── NotificationHelper.kt
│           │       └── PermissionUtils.kt
│           │
│           └── res/
│               ├── drawable/
│               ├── mipmap/
│               ├── values/
│               └── xml/
│                   └── accessibility_service_config.xml
│
├── gradle/
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Main Components

**MainActivity.kt**

Initializes the application's Jetpack Compose interface, manages notification permissions, and handles navigation to Android Settings or other applications.

**AppInfo.kt**

Defines the data model used to represent installed applications and their information.

**AppRepository.kt**

Retrieves installed applications using Android's PackageManager, filters selected system packages, and sorts application records.

**AppViewModel.kt**

Manages application listing, search queries, filtering, permission states, and force-stop operation results.

**AppScreen.kt**

Displays the main application interface, including search functionality, installed applications, filtering options, and dialogs.

**AppActionSheet.kt**

Provides application-specific actions, including Force Stop and Relaunch.

**ForceStopAccessibilityService.kt**

Monitors relevant Accessibility events and Settings interface changes to infer the outcome of a manually initiated force-stop operation.

**ForceStopBridge.kt**

Manages communication between the application interface and the Accessibility Service.

**NotificationHelper.kt**

Creates notification channels and displays notifications for detected force-stop outcomes.

**PermissionUtils.kt**

Provides utilities for checking permissions and opening system permission settings.

## 🚀 Installation and Setup

### Prerequisites

- Android Studio
- Android SDK
- Compatible Java Development Kit (JDK)
- Android device or emulator running Android 8.0 (API 26) or later
- Git (optional)

### Step 1: Download the Project

Download the repository ZIP file from GitHub and extract it, or clone the repository using Git.

### Step 2: Open in Android Studio

1. Launch Android Studio.
2. Select **Open**.
3. Navigate to the extracted AppCustodian project folder.
4. Allow Gradle synchronization to complete.

### Step 3: Build the Application

Open the terminal in the project root and execute:

```bash
./gradlew assembleDebug
```

For Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK is normally located at:

`app/build/outputs/apk/debug/app-debug.apk`

### Step 4: Run the Application

1. Connect an Android device or start an emulator.
2. Select the target device in Android Studio.
3. Click the **Run** button.
4. The application launches and displays installed applications.

### Step 5: Enable Accessibility Service

1. Open **App Custodian**.
2. Follow the prompt to open Android Accessibility settings.
3. Find the App Custodian Accessibility Service.
4. Enable the service after reviewing the system permission information.
5. Return to the application.

**Optional permissions:**

- Enable **Usage Access** to sort applications by recent usage.
- Allow **Notifications** to receive force-stop outcome alerts.

## 📱 How to Use

1. Launch the **App Custodian** application.
2. Browse the list of installed applications.
3. Use the search bar to find an application by name or package identifier.
4. Use **Show system / Hide system** to filter system applications.
5. Select an application to view its available actions.
6. Choose **Relaunch** to open the selected application, if supported.
7. Choose **Force Stop** to navigate to the application's system App Info screen.
8. Manually tap **Force Stop** and confirm the action in Android Settings.
9. The enabled Accessibility Service monitors the Settings interface and attempts to detect the outcome.
10. Review the result through the in-app dialog or system notification, when available.

## ⚙️ How It Works

### 1. Installed Application Retrieval

The application uses Android's `PackageManager` to retrieve installed application information.

Each application record includes details such as:

- Application name
- Application icon
- Package identifier
- System application status
- Last-used timestamp, when permission allows

### 2. Search and Filtering

Users can search applications using their names or package identifiers.

The application supports filtering system applications and sorting records according to recent usage information when available.

### 3. Manual Force-Stop Workflow

Android restricts ordinary third-party applications from directly force-stopping other applications.

App Custodian provides a guided workflow:

1. The user selects an application and chooses **Force Stop**.
2. The application opens the corresponding Android **Settings → App Info** screen.
3. The user manually taps **Force Stop**.
4. The user confirms the action through the system confirmation dialog.
5. The Accessibility Service monitors the relevant changes in the Settings interface.
6. A detected result is displayed through the application interface or notification.

The Accessibility Service observes the workflow and does not simulate button presses.

### 4. Accessibility Service Monitoring

The `ForceStopAccessibilityService` monitors Accessibility events generated by Android Settings.

The service is configured to observe the system Settings package:

`com.android.settings`

It uses Accessibility node information and UI state changes to identify relevant force-stop controls and confirmation actions.

Monitoring is activated for a selected application after the user initiates the force-stop workflow.

### 5. Force-Stop Outcome Detection

The implementation periodically checks the Accessibility node hierarchy to determine the observed state of the force-stop interface.

The monitoring process uses:

- **Polling interval:** Approximately 500 milliseconds
- **Initial settling period:** Approximately 900 milliseconds for applicable checks
- **Timeout:** 60 seconds

| Outcome | Detection Behavior |
|---|---|
| Success | A confirmation action is detected, or the confirmation dialog disappears without an enabled Force Stop control remaining |
| Disabled | The Force Stop control remains disabled across consecutive checks |
| Not Present | The Force Stop control cannot be identified on an apparently loaded App Info screen |
| Timeout | No conclusive outcome is detected within 60 seconds |
| Cancelled | A detected cancellation action ends monitoring without reporting success |

After reporting an outcome, the service attempts to navigate back to App Custodian and provide feedback.

**Important:** Outcome detection is based on observed interface behavior. It does not provide system-level confirmation that a process has been terminated.

### 6. App Relaunch

The application uses Android launch intents to reopen selected applications.

The relaunch operation uses activity flags including:

```kotlin
Intent.FLAG_ACTIVITY_NEW_TASK
Intent.FLAG_ACTIVITY_CLEAR_TOP
Intent.FLAG_ACTIVITY_CLEAR_TASK
```

Applications without an available launcher activity cannot be reopened through the Relaunch option.

### 7. Notification Management

The application provides notifications containing detected force-stop results.

On Android 13 and later, notification permission must be granted for these notifications to appear.

## 🔐 Permissions

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | Retrieve installed application information |
| `PACKAGE_USAGE_STATS` | Access application usage information for sorting |
| `POST_NOTIFICATIONS` | Display operation result notifications on Android 13+ |
| Accessibility Service | Observe relevant Settings interface events during the force-stop workflow |

### Accessibility Service Usage

The Accessibility Service is explicitly enabled by the user through Android Settings.

It is designed to observe the manual force-stop procedure and provide feedback based on detected interface changes.

The service does not automatically interact with Force Stop or confirmation buttons.

## ⚠️ Known Limitations

### 1. Android Force-Stop Restrictions

Android does not allow ordinary third-party applications to directly force-stop other applications using privileged system APIs.

App Custodian therefore requires the user to perform the force-stop action manually.

### 2. Device and Manufacturer Compatibility

The Accessibility Service identifies controls using Android Settings resource identifiers and text matching.

Different manufacturers may use customized Settings interfaces.

Device interfaces such as Samsung One UI, MIUI, and ColorOS may require additional resource mappings or detection adjustments.

### 3. Language Compatibility

The implementation includes English-language text matching for controls such as **Force Stop**, **OK**, and **Cancel**.

Non-English system languages may affect detection accuracy.

### 4. Protected Applications

Some system applications cannot be force-stopped.

The project excludes selected protected packages, including:

- `android`
- `com.android.systemui`
- `com.android.settings`
- `com.android.phone`
- `com.android.shell`

This filtering is not exhaustive, and additional restricted applications may still appear.

### 5. No Live Process Monitoring

The application displays installed applications rather than a real-time list of running processes.

Recent usage information does not necessarily indicate that an application is currently running.

### 6. Outcome Detection Accuracy

The Accessibility Service determines outcomes from observed interface changes.

Results may vary depending on the Android version, device manufacturer, system language, and Settings interface.

### 7. Application Relaunch

Applications without a launcher activity cannot be relaunched using the app's Relaunch option.

### 8. Google Play Distribution

Permissions such as `QUERY_ALL_PACKAGES` and Accessibility Service access may require additional justification, disclosures, and policy compliance for Google Play distribution.

## 📦 APK and Release Builds

The repository includes a prebuilt APK:

`Apk/App_Custodian.apk`

A new debug APK can be generated using:

```bash
./gradlew assembleDebug
```

### Release Build

To generate a release build:

```bash
./gradlew assembleRelease
```

The project supports release signing configuration using a `key.properties` file in the root project directory.

If the signing configuration is unavailable, the release build is configured to produce an unsigned APK.

Release builds enable:

- Code shrinking
- Resource shrinking
- Code optimization and obfuscation through R8/ProGuard rules

## 📌 Project Information

| Detail | Description |
|---|---|
| Project Name | App Custodian |
| Application Type | Android Utility |
| Programming Language | Kotlin |
| Development Environment | Android Studio |
| UI Framework | Jetpack Compose |
| UI Components | Material 3 |
| Architecture | MVVM-style |
| Package Name | `com.appcustodian.app` |
| Minimum SDK | API 26 (Android 8.0) |
| Compile SDK | API 36 |
| Target SDK | API 36 |
| Application Version | `0.1.0-Alpha01` |
| Build System | Gradle Kotlin DSL |

## 🎯 Learning Outcomes

This project demonstrates:

- Developing Android applications using Kotlin.
- Designing modern interfaces using Jetpack Compose and Material 3.
- Applying MVVM-style architecture for UI state management.
- Retrieving installed application information using PackageManager.
- Implementing application searching, filtering, and sorting.
- Integrating Android Accessibility Services.
- Understanding Android application management restrictions.
- Handling system permissions and Settings navigation.
- Implementing notification channels and result notifications.
- Managing asynchronous operations using Kotlin Coroutines and StateFlow.
- Building Android applications using Gradle.

## 📝 Conclusion

**App Custodian** is a Kotlin-based Android utility application designed to simplify installed-application management and assist users with Android's manual force-stop procedure.

The application combines installed-app discovery, searching, filtering, relaunching, and Accessibility Service–assisted outcome monitoring within a modern Jetpack Compose interface.

It demonstrates practical Android platform integration while respecting system restrictions on directly force-stopping other applications.
