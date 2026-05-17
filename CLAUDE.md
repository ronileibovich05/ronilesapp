# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "com.example.ronilesapp.ExampleUnitTest"

# Clean build
./gradlew clean

# Lint
./gradlew lint
```

All source is Java (not Kotlin) under `app/src/main/java/com/example/ronilesapp/`.

## Architecture Overview

**Traditional Activity-driven MVC — no ViewModel, no Navigation component, no DI framework.**

### Navigation Flow
```
SplashActivity (launcher, 4s timer)
  ├── LoginActivity ↔ RegisterActivity
  └── TasksActivity (main hub, post-login)
        ├── CategoryTasksFragment × N  (ViewPager2 + TabLayout, one per category)
        ├── AddTaskActivity            (FAB; also handles edit mode via Intent extras)
        ├── ProfileActivity
        └── SettingsActivity
              └── AdminDashboardActivity (gated by User.isAdmin flag)
```

Navigation is entirely Intent-based with `ActivityResultLauncher` contracts.

### BaseActivity
All activities extend `BaseActivity`, which manages theme switching. It reads a `KEY_THEME` SharedPreference (`"pink_brown"` / `"blue_white"` / `"green_white"`) on `onCreate` and checks for changes in `onResume`, calling `recreate()` when needed. `TasksActivity` overrides this with an in-place `applyThemeColors()` to avoid full recreation.

### Firebase / Data Layer
`Utils.java` is the central Firebase singleton — it holds static references to `FirebaseAuth` (`mAuth`) and `FirebaseFirestore` (`FBFS`), plus helpers for user-scoped collection refs (`getUserTasksRef()`, `getUserCategoriesRef()`) and connectivity checks. There is no local database; all persistence is Firestore with real-time `SnapshotListener`s.

### Key Data Models
- **UserTask** — fields: `id`, `title`, `description`, `day/month/year/hour/minute`, `category`, `done`, `position`, `creationTime`. Helper `getTimeInMillis()` used for alarm scheduling.
- **User** — fields: `uid`, `firstName`, `lastName`, `email`, `notifications`, `profileImageUrl`, `isAdmin`.
- **Category** — simple POJO with `name`.
- **SharedTask** — task sharing metadata (`id`, `senderEmail`, `receiverEmail`, `taskId`).

### Notifications
`NotificationHelper` schedules alarms via `AlarmManager` at the task's due time; `NotificationReceiver` (BroadcastReceiver) fires the notification and opens `TasksActivity`. Notification IDs are derived from `taskId.hashCode()`. Notifications respect the per-user `notifications` flag in SharedPreferences.

### Image Handling
Two separate approaches are used:
- **Registration:** Profile image encoded as Base64 and stored directly in Firestore.
- **Profile updates:** Image uploaded to Firebase Storage; URL stored in Firestore and loaded with Glide.

### Themes
Three XML theme variants: `Theme_PinkBrown` (default), `Theme_BlueWhite`, `Theme_GreenWhite`, each with a `-night` dark mode counterpart. Defined in `res/values/themes.xml` and `res/values-night/themes.xml`.

## Notable Conventions
- Code comments are written in Hebrew.
- String resources include both Hebrew and English strings.
- `google-services.json` is committed at `app/google-services.json` — contains Firebase project config.
- Min SDK is 27; target/compile SDK is 35.
