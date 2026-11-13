# HabitFlow — Android Habit Tracker

**HabitFlow** is an Android habit-tracking application built with Kotlin, Jetpack Compose (Material Design 3), Room local persistence, daily scheduled notifications (`AlarmManager`), and progress analytics.

- **Package / Application ID:** `com.aistudio.habitflow.zqvxm`
- **Minimum SDK:** Android 7.0 (API 24)
- **Target / Compile SDK:** API 36

---

## Project Structure

```text
├── .github/workflows/android-apk.yml   # CI/CD workflow for APK builds & GitHub Releases
├── app/
│   ├── build.gradle.kts                # App module Gradle configuration & signingConfigs
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # Permissions & HabitReminderReceiver registration
│           ├── java/com/example/
│           │   ├── MainActivity.kt     # Entry point, Compose navigation & permission flow
│           │   ├── data/               # Room database, DAOs, entities & repository
│           │   ├── notification/       # NotificationChannel & AlarmManager scheduling
│           │   ├── receiver/           # BroadcastReceiver for daily & boot reminders
│           │   └── ui/                 # Screens (Today, Analytics), dialogs & M3 theme
│           └── res/                    # Adaptive launcher icons, strings & XML configs
├── build.gradle.kts                    # Top-level Gradle plugins
├── gradle/libs.versions.toml           # Version catalog
├── gradle.properties                   # Gradle & Kotlin build settings
└── settings.gradle.kts                 # Project settings
```

---

## Automated Builds & GitHub Releases (GitHub Actions)

The workflow at `.github/workflows/android-apk.yml` runs automatically on every push to `main` / `master`, on `v*` tags, on pull requests, and via manual dispatch:

1. Restores `debug.keystore` from `debug.keystore.base64` and builds `HabitFlow-debug.apk` (`:app:assembleDebug`).
2. Builds `HabitFlow-release.apk` (`:app:assembleRelease`) using your configured GitHub Secrets (or an ephemeral CI upload keystore if secrets are not yet set).
3. Uploads both APKs as workflow artifacts and publishes them directly to **GitHub Releases**.

### Configuring Your Permanent Release Signing Key (Recommended)

To sign `HabitFlow-release.apk` with your permanent upload key (alias `upload`), add these three secrets under **Settings → Secrets and variables → Actions** in your GitHub repository:

| Secret Name | Value |
| --- | --- |
| `KEYSTORE_BASE64` | Base64-encoded contents of your `.jks` keystore (`base64 -w 0 my-upload-key.jks`) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password for the `upload` alias |

---

## Building the APK Locally with Gradle

Ensure JDK 17+, the Android SDK (`ANDROID_SDK_ROOT`), and Gradle 9.3.1 are installed.

1. **Restore the debug keystore** (first time only):
   ```bash
   base64 -d debug.keystore.base64 > debug.keystore
   ```
2. **Build the Debug APK**:
   ```bash
   gradle :app:assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`
3. **Build the Signed Release APK**:
   ```bash
   export KEYSTORE_PATH=/path/to/my-upload-key.jks
   export STORE_PASSWORD=your_store_password
   export KEY_PASSWORD=your_key_password
   gradle :app:assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`
