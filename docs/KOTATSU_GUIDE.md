# Kotatsu Manga Reader — Developer Guide
>
> Written for Flutter developers with no prior Android/Kotlin experience.

---

## Table of Contents

1. [What is this project?](#1-what-is-this-project)
2. [Tech stack — Flutter vs Android](#2-tech-stack--flutter-vs-android)
3. [Project structure](#3-project-structure)
4. [Architecture pattern](#4-architecture-pattern)
5. [Key packages explained](#5-key-packages-explained)
6. [Key files to know](#6-key-files-to-know)
7. [How a feature is built (example)](#7-how-a-feature-is-built-example)
8. [Making changes to the code](#8-making-changes-to-the-code)
9. [Creating a release build (production APK)](#9-creating-a-release-build-production-apk)
10. [Keystore — what it is and what to do if missing](#10-keystore--what-it-is-and-what-to-do-if-missing)
11. [Installing on your device](#11-installing-on-your-device)

---

## 1. What is this project?

Kotatsu is a **native Android manga reader app** written in Kotlin. It was originally developed and maintained publicly on GitHub but was shut down by its owners due to legal pressure. The source code remains fully intact and functional.

**What the app does:**

- Reads manga from 1200+ online sources
- Supports offline downloads (CBZ/ZIP)
- Organizes manga into favorite categories
- Tracks reading history
- Syncs with trackers like AniList, MyAnimeList, Shikimori
- Supports cross-device cloud sync and encrypted backups

---

## 2. Tech stack — Flutter vs Android

If you know Flutter, think of these as equivalents:

| Concept | Flutter | Android (Kotatsu) |
| --- | --- | --- |
| Language | Dart | Kotlin |
| UI components | Widgets | Views / Fragments / Activities |
| Screen navigation | Navigator / GoRouter | Fragment back stack / AppRouter |
| State management | Provider / Riverpod | ViewModel + StateFlow |
| Reactive streams | Stream / StreamController | Flow / StateFlow |
| Async/await | `Future` / `async-await` | Coroutines / `suspend fun` |
| Dependency injection | GetIt / Riverpod | Hilt (compile-time DI) |
| Database | Drift / Hive | Room (ORM, SQLite) |
| HTTP client | Dio / http | OkHttp |
| Image loading | CachedNetworkImage | Coil |
| Background tasks | background_fetch / Isolates | WorkManager |
| App config/prefs | SharedPreferences | DataStore / SharedPreferences |
| Build file | `pubspec.yaml` | `app/build.gradle` |
| Dependency versions | `pubspec.yaml` | `gradle/libs.versions.toml` |

---

## 3. Project structure

Everything lives in a single module (`app/`). There are no separate Flutter-style packages — all features are sub-packages of the main app.

```text
app/src/main/
├── kotlin/org/koitharu/kotatsu/
│   ├── core/               # Shared infrastructure (DB, network, UI base classes)
│   ├── main/               # App entry point, main activity
│   ├── reader/             # Manga reading screen
│   ├── details/            # Manga detail/info screen
│   ├── favourites/         # Favorites library + categories
│   ├── history/            # Reading history
│   ├── explore/            # Browse and discover manga sources
│   ├── search/             # Search functionality
│   ├── download/           # Offline download manager
│   ├── local/              # Local file support (CBZ/ZIP)
│   ├── settings/           # App settings screens
│   ├── bookmarks/          # Chapter bookmarks
│   ├── tracker/            # Tracker integration (AniList, MAL, etc.)
│   ├── scrobbling/         # Auto-mark chapters as read on trackers
│   ├── backups/            # Backup and restore
│   ├── sync/               # Cross-device sync
│   ├── suggestions/        # Manga recommendations
│   ├── stats/              # Reading statistics
│   └── widget/             # Home screen widgets
└── res/
    ├── layout/             # XML UI layouts (like Flutter widget trees, but in XML)
    ├── menu/               # Menu XML files (popup menus, action bars)
    ├── values/             # strings.xml, colors.xml, themes
    └── drawable/           # Icons and images
```

---

## 4. Architecture pattern

Kotatsu uses **MVVM + Clean Architecture** — similar to how Flutter apps use BLoC or Riverpod with repositories.

```text
UI Layer  →  ViewModel  →  Repository  →  Data Source (DB / Network)
```

**Flutter analogy:**

```text
Widget  →  Provider/Riverpod notifier  →  Repository  →  API / local DB
```

### Data flow example (loading favorites)

```text
FavouritesListFragment          (like a Flutter StatefulWidget)
    ↓ observes
FavouritesListViewModel         (like a Riverpod AsyncNotifier)
    ↓ calls
FavouritesRepository            (like a Flutter repository class)
    ↓ queries
FavouritesDao (Room)            (like a Drift DAO)
    ↓ reads
SQLite database
```

### Key concepts

**ViewModel** — holds UI state, survives screen rotation. Exposes `StateFlow` (like a Riverpod `StateProvider`) that the Fragment observes.

**Repository** — the single source of truth. Talks to the DB or network and returns domain models.

**Flow** — Kotlin's equivalent of Dart `Stream`. Emits values over time. `StateFlow` is like a `StreamController.broadcast()` with a current value.

**Coroutines** — Kotlin's equivalent of Dart `async/await`. `launchJob {}` inside a ViewModel is like calling an async function.

**Hilt** — automatically injects dependencies (like repositories into ViewModels). You never call `new SomeRepository()` manually.

---

## 5. Key packages explained

### `core/`

Shared infrastructure used by every feature.

| Sub-package | What it does |
| --- | --- |
| `core/db/` | Room database definition, all DAOs, 27 migration files |
| `core/model/` | Domain data classes (`Manga`, `FavouriteCategory`, etc.) |
| `core/network/` | OkHttp setup, interceptors (CloudFlare bypass, rate limiting, DNS-over-HTTPS) |
| `core/prefs/` | `AppSettings.kt` — all user preferences in one place |
| `core/ui/` | Base classes: `BaseActivity`, `BaseFragment`, `BaseViewModel` |
| `core/util/` | Kotlin extension functions, helpers |
| `core/nav/` | `AppRouter` — handles all navigation between screens |

### `favourites/`

Everything related to the favorites library.

| Sub-package | What it does |
| --- | --- |
| `favourites/data/` | Room entities, DAOs, DB-to-model mapping |
| `favourites/domain/` | `FavouritesRepository` — business logic |
| `favourites/ui/container/` | The tabbed favorites screen with category tabs |
| `favourites/ui/categories/` | The "Manage Categories" screen |
| `favourites/ui/list/` | The list of manga inside a single category |

### `reader/`

The core reading experience — page rendering, gestures, zoom, webtoon mode.

### `parsers` (external library)

Manga sources (websites/APIs) are handled by an external library `kotatsu-parsers`. The `Manga` model class comes from this library.

---

## 6. Key files to know

| File | What it does |
| --- | --- |
| `app/build.gradle` | App version, build types, signing config, dependencies |
| `gradle/libs.versions.toml` | All dependency versions (like `pubspec.yaml` lock file) |
| `core/prefs/AppSettings.kt` | Every user preference, 849 lines |
| `core/db/MangaDatabase.kt` | Room database with all tables and DAOs |
| `core/AppModule.kt` | Hilt wiring — provides singletons like DB, OkHttp, Coil |
| `core/nav/AppRouter.kt` | All navigation calls in one place |
| `core/util/ShareHelper.kt` | Sharing text/files via Android share sheet |
| `favourites/domain/FavouritesRepository.kt` | All favorites data operations |

---

## 7. How a feature is built (example)

Here's the export feature added to this fork as a reference. When you long-press a category tab → "Export list":

**1. Menu XML** — `res/menu/popup_fav_tab.xml`
Added a new `<item>` with `id="action_export"`.

**2. Menu handler** — `FavouriteTabPopupMenuProvider.kt`
Added `R.id.action_export -> viewModel.exportCategory(categoryId)` in the `when` block.

**3. ViewModel** — `FavouritesContainerViewModel.kt`

- Added `val onExportReady = MutableEventFlow<String>()` — the event channel
- Added `fun exportCategory(categoryId)` — fetches manga from DB via repository, builds plain text, fires the event

**4. Fragment** — `FavouritesContainerFragment.kt`
Observes `onExportReady` and opens Android's share sheet with the text.

**Pattern to follow for any new feature:**

1. Add strings in `res/values/strings.xml`
2. Add UI (menu item, button, layout) in `res/`
3. Add business logic in the ViewModel using `launchJob {}` or `launchLoadingJob {}`
4. Call the repository for data operations
5. Emit results via `MutableEventFlow` or `MutableStateFlow`
6. Observe in the Fragment and update UI

---

## 8. Making changes to the code

After any code change, you do not need to do anything special before building. Gradle handles compilation automatically when you run a build command.

**Quick checklist before building:**

- [ ] Did you add a new string? Add it to `res/values/strings.xml`
- [ ] Did you add a new database table or change an existing one? You need a Room migration (advanced — ask separately)
- [ ] Did you change only UI/logic (no DB schema changes)? You are good to go

---

## 9. Creating a release build (production APK)

Do this every time you want to install an updated version.

### Step 1 — Bump the version (optional but recommended)

Open `app/build.gradle` and increment both values:

```groovy
versionCode = 1035          // always +1 from previous
versionName = '9.4.3'       // human-readable, your choice
```

> `versionCode` must always go up. Android uses it to know if an APK is newer.

### Step 2 — Make sure your keystore is in place

The keystore file must exist at the project root:

```text
Kotatsu-Manga-Reader/release.jks
```

The signing config in `app/build.gradle` should look like this:

```groovy
signingConfigs {
    release {
        storeFile rootProject.file('release.jks')
        storePassword 'kotatsu123'
        keyAlias 'kotatsu'
        keyPassword 'kotatsu123'
    }
}
```

> If the keystore is missing, see [Section 10](#10-keystore--what-it-is-and-what-to-do-if-missing).

### Step 3 — Build the APK

Run this from the project root in your terminal:

```bash
./gradlew assembleRelease
```

The signed APK will be at:

```text
app/build/outputs/apk/release/app-release.apk
```

### Step 4 — Install on your connected device

Connect your phone via USB with USB Debugging enabled, then run:

```bash
./gradlew installRelease
```

This builds **and** installs in one step.

Or install manually from the APK file:

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 10. Keystore — what it is and what to do if missing

### What is a keystore?

A keystore is a file that contains your private signing key. Android uses it to verify that APK updates come from the same author. Think of it like a digital signature.

- If you install an APK signed with key A, only future APKs also signed with key A will be accepted as updates
- If the keys differ, Android rejects the update and you must uninstall first
- The keystore for this project is `release.jks` in the project root

### Scenario 1 — `release.jks` exists (normal case)

Nothing to do. Just run `./gradlew assembleRelease` or `./gradlew installRelease`.

### Scenario 2 — `release.jks` is missing (first time or lost)

Generate a new one with this command:

```bash
keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias kotatsu \
  -storepass kotatsu123 \
  -keypass kotatsu123 \
  -dname "CN=Kotatsu, OU=Personal, O=Personal, L=Unknown, ST=Unknown, C=US"
```

This creates `release.jks` in whatever directory you run the command from. Run it from the project root.

Then verify `app/build.gradle` has the signing config pointing to it:

```groovy
signingConfigs {
    release {
        storeFile rootProject.file('release.jks')
        storePassword 'kotatsu123'
        keyAlias 'kotatsu'
        keyPassword 'kotatsu123'
    }
}
```

> If you generate a **new** keystore (because you lost the old one), any device that has your previous build installed must **uninstall it first** before installing the new APK. The keys will not match.

### Scenario 3 — `keytool` command not found

`keytool` comes with Java/JDK. Install it if missing:

**macOS:**

```bash
brew install openjdk
```

**Check it works:**

```bash
keytool -help
```

### Keep your keystore safe

- Back it up to iCloud, Google Drive, or a USB drive
- Do **not** commit it to a public Git repository (it contains your private key)
- Add it to `.gitignore`:

  ```text
  release.jks
  ```

---

## 11. Installing on your device

### First-time install (or after switching keystores)

```bash
# Uninstall the existing app first (will delete app data)
adb uninstall org.koitharu.kotatsu

# Install your build
adb install app/build/outputs/apk/release/app-release.apk
```

> **Back up your data first** — open the existing app → Settings → Backup & Restore → Create backup. After reinstalling, restore from that backup.

### Updating (same keystore, version code increased)

```bash
./gradlew installRelease
```

Android will treat it as a seamless update. No data loss.

### Sharing the APK with others (internal use)

Share the file at:

```text
app/build/outputs/apk/release/app-release.apk
```

Via WhatsApp, Telegram, Google Drive, email, etc.

The recipient must:

1. Enable **Install from unknown sources** in Android Settings
2. Uninstall the original Kotatsu app if they have it (key mismatch)
3. Open the shared APK file to install

---

## Full release workflow summary

```text
1. Make your code changes
2. Bump versionCode and versionName in app/build.gradle
3. Confirm release.jks exists in project root
4. Run: ./gradlew installRelease
5. Done — app is built, signed, and installed
```
