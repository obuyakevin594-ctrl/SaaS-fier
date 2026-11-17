# DeepSleep - Native Android Project Plan

DeepSleep is a high-performance native Android application written in Kotlin using standard XML views. The application puts selected user-installed apps into deep hibernation by automating the Android system "Force Stop" sequence via an Accessibility Service, halting persistent background services, wake locks, and storage/cache growth.

---

## 1. Core Architecture & Components

### 1. Main Dashboard (`MainActivity.kt` & `activity_main.xml`)
- **Package Scanning:** Queries all non-system installed apps using `PackageManager` and the `QUERY_ALL_PACKAGES` permission, excluding system/framework packages (`FLAG_SYSTEM == 0`).
- **Data Display:** Displays app icon, user-friendly application label, package name, and a selection checkbox in a smooth `RecyclerView`.
- **Search & Quick Toggles:** Provides instant search filtering and "Select All" / "Deselect All" options.
- **Persistence (`SleepPreferences.kt`):** Stores selected package names in `SharedPreferences` as a `Set<String>`, persisting choices across device reboots and app launches.

### 2. Transparent Disclosure & Permission Flow
- **Accessibility Status Card:** A prominent card at the top of the main screen indicating live service status (`Active` in green or `Inactive` in amber) with an "Enable Deep Sleep" button.
- **Prominent Disclosure Dialog (`dialog_prominent_disclosure.xml`):** Strictly conforms to Google Play User Data & Accessibility guidelines. Explains:
  - How the Accessibility Service is used solely to automate the manual "Force Stop" button clicks in system Settings.
  - No personal data, keystrokes, messages, or credentials are read, saved, or sent off the device.
  - Direct deep link to `Settings.ACTION_ACCESSIBILITY_SETTINGS`.

### 3. Automation Engine (`HibernateAccessibilityService.kt` & `HibernationManager.kt`)
- **Sequential Queue Automation:** Reads the user's Sleep List and opens each app's `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` one at a time.
- **OEM Layout Compatibility:**
  - Standard AOSP / Google Pixel: `com.android.settings:id/button1`, `com.android.settings:id/force_stop_button`, `com.android.settings:id/right_button`.
  - Xiaomi MIUI / HyperOS: `com.miui.securitycenter:id/force_stop` and `button1`.
  - Transsion HiOS / XOS: Dynamic text matching and `com.transsion` / `com.android.settings` view trees.
  - Multi-language fallback: Case-insensitive search for keywords like "Force stop", "Force close", "Forcer l'arrêt", "Stoppen erzwingen", "Detener", "Forçar parada", "Arresto forzato".
- **Smart Skip:** If the "Force Stop" button is already disabled (`node.isEnabled == false`), the engine knows the app is already hibernated and skips immediately to the next app without waiting for popups.
- **Confirmation Dialog Handling:** Automatically searches and clicks the confirmation popup button (`android:id/button1` or "OK" / "Force stop").
- **Timeout Watchdog:** Recovers automatically if a settings screen takes too long or fails to render, continuing through the remaining queue.

### 4. One-Tap Trigger
- **Execution Bar:** A primary button labeled "Hibernate Selected Apps (X)" at the bottom of the screen.
- **Feedback & Return:** Launches the automated queue, displays toast updates (`Hibernating X of N...`), and automatically returns the user to DeepSleep when complete with a summary report.

---

## 2. Directory Layout

```
DeepSleep/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── drawable/
        │   │   ├── ic_moon.xml
        │   │   ├── ic_shield_check.xml
        │   │   ├── ic_shield_alert.xml
        │   │   ├── ic_search.xml
        │   │   ├── ic_play.xml
        │   │   └── bg_card.xml
        │   ├── layout/
        │   │   ├── activity_main.xml
        │   │   ├── item_app_sleep.xml
        │   │   └── dialog_prominent_disclosure.xml
        │   ├── values/
        │   │   ├── colors.xml
        │   │   ├── strings.xml
        │   │   └── themes.xml
        │   └── xml/
        │       └── accessibility_service_config.xml
        └── java/com/deepsleep/app/
            ├── model/
            │   └── AppInfo.kt
            ├── data/
            │   └── SleepPreferences.kt
            ├── service/
            │   ├── HibernationManager.kt
            │   └── HibernateAccessibilityService.kt
            ├── ui/
            │   ├── AppAdapter.kt
            │   └── MainActivity.kt
            └── utils/
                └── AccessibilityUtils.kt
```
