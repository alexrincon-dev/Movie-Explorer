# MovieExplorer

Android app for browsing TMDB's **Popular** and **Top Rated** movies, built for a development challenge.

## Features

- Popular and Top Rated lists in a two-column poster grid, with infinite scrolling and pull-to-refresh.
- Movie detail screen: backdrop, poster, original title, year, rating, release date and overview.
- Offline-first: cached movies remain visible without a connection, and failed requests never remove cached data.
- Movie content language selector (English / Spanish) for TMDB text.
- Adaptive layout: phones use list → detail navigation; windows at least 840dp wide show a list/detail two-pane layout.
- Material 3 light and dark themes. The UI is translated to English and Spanish.
- Each list keeps its own scroll position across navigation, rotation and phone/tablet layout changes.

## Tech stack

| Area | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, Navigation Compose (type-safe routes) |
| Presentation | Architecture Components ViewModel, `StateFlow` |
| Dependency injection | Hilt (KSP) |
| Persistence | Room (KSP), with exported schemas in `app/schemas` |
| Networking | Retrofit, OkHttp, kotlinx.serialization |
| Images | Coil 3 |
| Async | Kotlin Coroutines and Flow |
| Tests | JUnit 4, kotlinx-coroutines-test, Robolectric, AndroidX Test |

## Architecture

Clean Architecture with MVVM in a single `app` module. Package root: `dev.alexrincon.movieclient`.

```
core/            Hilt modules for networking (Retrofit, OkHttp, API key) and coroutine dispatchers
domain/          Pure Kotlin: models, MovieError, MovieRepository contract, use cases
data/
  local/         Room database, entities, DAOs, entity mappers and merge rules
  remote/        Retrofit API, DTOs, TMDB remote data source
  image/         TMDB image URL building
  repository/    MovieRepositoryImpl (offline-first) and error translation
presentation/
  movielist/     List screen, poster grid, MovieListViewModel
  moviedetail/   Detail screen, MovieDetailViewModel
  navigation/    NavHost, routes, adaptive master-detail layout
  language/      In-memory movie content language state
ui/theme/        Material 3 theme
```

- **Domain** has no Android, Room, Retrofit or Compose dependencies.
- **Data** implements `MovieRepository`. DTOs and Room entities never leave the data layer.
- **Presentation** observes domain models through ViewModels that expose immutable `StateFlow` UI state.

## Offline-first behavior

- Room is the single source of truth for the UI. Screens observe the database, and network responses are written to Room.
- Network calls happen outside database transactions. Each successful response is written atomically: movies, list membership and pagination.
- Popular and Top Rated memberships are stored independently, so refreshing one list never removes the other.
- Refreshing a list replaces it with page 1. Infinite scrolling appends later pages and skips duplicates.
- Fields missing from a response (poster, backdrop, release date, rating) do not erase cached values.
- Opening a detail refreshes that movie's data without changing list membership.
- On failure, the cache is left unchanged and the UI shows a retryable error, keeping cached content visible.

## Movie content language vs. app language

There are two independent language settings:

- **App UI language** follows the Android system locale through string resources (`values/` and `values-es/`).
- **Movie content language** is selected with the **EN/ES** button in the list's top bar. It controls the `language` parameter sent to TMDB (`en-US` or `es-MX`), which affects text such as overviews.

Changing the content language refreshes page 1 of both lists in the new language. The selection is published only after both requests succeed; if either fails, the previous language and cached data are kept. The app displays TMDB's *original title*, which TMDB does not translate.

## TMDB API setup

1. Create a TMDB account and request an API key (v3 auth) at <https://www.themoviedb.org/settings/api>.
2. Add the key to `local.properties` in the project root, creating the file if needed:

   ```properties
   TMDB_API_KEY=your_key_here
   ```

3. Do not commit `local.properties`. It is already listed in `.gitignore`.

The build injects the key into `BuildConfig.TMDB_API_KEY`, which keeps it out of source control. It does **not** make the key secret: any key embedded in an APK can be extracted. For a production app, requests should go through a backend that holds the key.

If the key is missing or invalid, the project still compiles. TMDB then rejects every request with HTTP 401, and the app shows a message saying the TMDB API key is missing or invalid.

## Build and run

Requirements:

- A recent Android Studio that supports AGP 9.5.
- Android SDK 37.
- A device or emulator running Android 9 (API 28) or later.

The Gradle daemon JVM is defined in `gradle/gradle-daemon-jvm.properties` and is provisioned automatically. Code targets Java 17.

Open the project in Android Studio and run the `app` configuration, or use the command line:

```bash
./gradlew :app:assembleDebug      # build the debug APK
./gradlew :app:installDebug       # install on a connected device or emulator
./gradlew :app:assembleRelease    # R8-optimized release APK (unsigned)
```

The release build is not configured with a signing key, so it produces `app-release-unsigned.apk`.

## Tests

```bash
./gradlew :app:testDebugUnitTest            # JVM unit tests
./gradlew :app:connectedDebugAndroidTest    # instrumented tests, needs a device or emulator
./gradlew :app:compileDebugKotlin           # quick compile check
```

- **Unit tests** (`app/src/test`): ViewModels with a fake repository, entity/DTO mappers and merge rules, error translation, remote data source, image URLs, adaptive layout helpers, date formatting.
- **Instrumented tests** (`app/src/androidTest`):
  - `MovieRepositoryImplTest`: repository behavior against an in-memory Room database, including failure handling, pagination and concurrency.
  - `MovieGridStatesTest`: Compose state restoration of the grid scroll positions.

`MovieGridStatesTest` uses the Compose test rule, which depends on Espresso. With the project's current `espresso-core` version (3.5.1), it fails on API 34+ devices (`InputManager.getInstance`). The repository tests do not use Espresso and run on current devices.

## Known limitations

- The selected movie content language is kept in memory only. After the app process restarts it returns to English, while cached movies may still contain Spanish text until they are refreshed.
- Each movie stores one overview, in the language of its latest successful response.
- Cached movies are never pruned. The cache only grows as more pages are loaded.
- There is no background sync. Data refreshes when a list is first opened, on pull-to-refresh, on language change, and when a detail is opened.
- The two-pane layout depends only on window width (840dp or more), so some large phones also use it in landscape.
- No release signing configuration is included.
