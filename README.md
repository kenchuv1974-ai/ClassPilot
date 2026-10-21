# ClassPilot — Attendance & Smart Schedule

[![License: All Rights Reserved](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](LICENSE)
[![Author](https://img.shields.io/badge/Author-Harshith%20V-00E5FF.svg)](https://github.com/kenchuv1974-ai)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg)](#)
[![Offline](https://img.shields.io/badge/Offline-100%25%20Independent-blueviolet.svg)](#)

> **⚡ Powered By Harshith** — All Rights Reserved. This repository is publicly visible for code review and portfolio demonstration. Unauthorized copying, distribution, rebranding, and academic plagiarism are strictly prohibited.

## Overview

**ClassPilot** is a modern, fully offline native Android attendance and smart timetable manager built with Kotlin and Jetpack Compose. It stores 100% of its data locally using Room SQLite and DataStore — requiring zero internet permissions, no cloud, no ads, and no tracking.

---

## Features

| Feature | Details |
|---|---|
| Dashboard | Greeting, next class countdown, today's attendance, warnings |
| Today's Classes | Full timeline with mark-present/absent per class |
| Tomorrow's Classes | Automatic day-change, first class preview |
| Weekly Timetable | Per-day editor with overlap validation |
| Smart Reminders | AlarmManager with 5/10/15/30/60-min presets, reboot-safe |
| Daily Notifications | Morning timetable + evening tomorrow preview |
| Attendance Tracking | Per-subject present/absent/cancelled, history |
| Attendance Calculator | Can-miss / need-to-attend predictor with custom target slider |
| Calendar | Monthly grid, holiday/exam/assignment events |
| Backup & Restore | JSON export/import via Android Storage Access Framework |
| Dark/Light/System theme | Full Material 3 theming |
| Settings | Name, theme, threshold, notification control, diagnostic |

---

## How to Build

### Prerequisites

1. **Download Android Studio** (free): https://developer.android.com/studio
   - Bundles JDK 17, Android SDK, and Gradle — no separate installs needed.
2. **Android SDK level 35** (auto-installed by Android Studio on first launch).

### Steps to Get the APK

```
1. Open Android Studio
2. File → Open → select the cloned ClassPilot project folder
3. Wait for Gradle sync to finish (first time ~3–5 min, downloads dependencies)
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
   OR press Shift+F10 to build and run directly on a connected phone
5. The debug APK will be at:
   app/build/outputs/apk/debug/app-debug.apk
```

### Transfer APK to Phone

1. Connect phone via USB, enable "File Transfer" mode.
2. Copy `app-debug.apk` to the phone.
3. On the phone: Settings → Install unknown apps → allow your file manager.
4. Tap the APK and install.

> **Signed Release APK** (optional, for distribution):
> Build → Generate Signed Bundle/APK → APK → create a keystore → Release.
> This produces `app-release.apk` which is smaller and optimized.

---

## Project Structure

```
ClassPilot/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── kotlin/com/classpilot/offline/
│   │       ├── ClassPilotApp.kt          # Application class (Hilt entry point)
│   │       ├── MainActivity.kt           # Main activity with nav + theme
│   │       ├── backup/                   # Backup/Restore JSON manager
│   │       ├── data/
│   │       │   ├── database/             # Room DB, entities, DAOs
│   │       │   ├── preferences/          # DataStore preferences
│   │       │   └── repository/           # Repository layer
│   │       ├── di/                       # Hilt DI modules
│   │       ├── domain/model/             # Pure domain models
│   │       ├── notifications/            # Channels, AlarmManager scheduler
│   │       ├── receivers/                # AlarmReceiver, BootReceiver, DailyReceiver
│   │       ├── scheduling/               # ReminderRescheduler (post-boot)
│   │       ├── ui/
│   │       │   ├── components/           # Shared Compose components
│   │       │   ├── navigation/           # Screen routes + NavGraph
│   │       │   ├── screens/              # All feature screens
│   │       │   └── theme/               # Material 3 theme, colors, typography
│   │       └── viewmodels/               # MVVM ViewModels
│   └── src/test/                         # JVM unit tests
├── gradle/
│   ├── libs.versions.toml               # Version catalog
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md                            ← you are here
```

---

## Architecture

- **MVVM** — ViewModels expose `StateFlow` / `Flow` to Compose UI
- **Room** — SQLite database with entities for Subject, TimetableEntry, AttendanceRecord, CalendarEvent
- **DataStore** — App preferences (theme, notification times, student name)
- **Hilt** — Dependency injection throughout
- **AlarmManager** — Precise class reminders (falls back to inexact if exact-alarm permission denied)
- **BroadcastReceiver** — Boot receiver reschedules all alarms after reboot
- **kotlinx.serialization** — JSON backup format

---

## Permissions Explained

| Permission | Why needed |
|---|---|
| `POST_NOTIFICATIONS` | Show class reminders and daily summaries |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after device reboot |
| `SCHEDULE_EXACT_ALARM` | Fire reminders at precise times (Android 12+) |
| `USE_EXACT_ALARM` | Alternative exact alarm mechanism (Android 13+) |
| `VIBRATE` | Vibration on notification (if enabled) |
| `WAKE_LOCK` | Ensure WorkManager wakes device briefly for scheduling |
| `FOREGROUND_SERVICE` | Required by Android for certain background operations |

**No INTERNET permission is declared or used.**

---

## Notification Platform Notes

- **Doze mode**: Android may delay alarms by several minutes when the device is still and unplugged. This is an OS-level restriction that cannot be fully bypassed.
- **OEM restrictions**: Xiaomi (MIUI), OPPO (ColorOS), Vivo, and Samsung ONE UI have additional battery managers that may suppress background alarms. Go to Settings → Battery → App Management → ClassPilot → No Restrictions to whitelist the app.
- **Exact alarms on Android 12+**: If the user declines `SCHEDULE_EXACT_ALARM`, reminders fall back to `setWindow()` (inexact, within ~10 min window). The Settings → Notification Diagnostic screen shows the current status.

---

## Running Unit Tests

```bash
# In Android Studio terminal or any terminal with JDK in PATH:
./gradlew :app:test

# Output at:
# app/build/reports/tests/testDebugUnitTest/index.html
```

Tests cover: attendance percentage, classes needed, can-miss, tomorrow date, overlap detection, calculator formulas.

---

## Backup File Format

```json
{
  "version": 1,
  "createdAt": 1729123456789,
  "appVersion": "1.0.0",
  "subjects": [...],
  "timetableEntries": [...],
  "attendanceRecords": [...],
  "calendarEvents": [...]
}
```

---

## 📦 Pre-Compiled Deliverable
- **Application Package**: `com.classpilot.offline`
- **Release Version**: `v1.0.0`
- **Architecture**: 100% Offline, Signed Android APK (`12.4 MB`)
- **Compatibility**: Android 8.0 (API 26) through Android 15 (API 35)

---

## 📄 License & Intellectual Property Rights

**Copyright © 2026 Harshith V ([@kenchuv1974-ai](https://github.com/kenchuv1974-ai)). All Rights Reserved.**

This repository is **Source-Available** and publicly visible for demonstration, code inspection, and portfolio evaluation purposes.

> [!CAUTION]
> **STRICT PROHIBITION OF UNAUTHORIZED COPYING & REDISTRIBUTION:**
> 1. **No Fork-and-Rebrand / Plagiarism**: You may **NOT** copy, mirror, or repackage this project and submit it as your own work for academic submissions, college capstone/final year projects, competitions, or certifications.
> 2. **No App Store Publishing**: You may **NOT** publish, monetize, or upload this application or its derivative builds to the Google Play Store, F-Droid, Amazon Appstore, or third-party marketplaces.
> 3. **No Commercial Use**: Any commercial exploitation or sale of this source code or design assets is strictly forbidden without express written authorization.
> 4. **Watermark & Attribution**: The in-app branding and watermark **"⚡ Powered By Harshith"** must remain intact and unaltered in all distributions.

For licensing permissions or business inquiries, reach out at **`kenchuv1974@gmail.com`**.

---

*ClassPilot — Crafted with modern Jetpack Compose & Clean Architecture. Powered By Harshith.*
