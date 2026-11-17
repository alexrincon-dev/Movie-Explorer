# MovieExplorer

Android app for browsing TMDB's **Popular** and **Top Rated** movies, built for a development challenge.

## Features

- Popular and Top Rated lists in a two-column poster grid, with infinite scrolling and pull-to-refresh.
- Movie detail screen: backdrop, localized title, original title (only when it differs from the localized title), poster, year, rating, release date and overview.
- Offline-first: cached movies remain visible without a connection, and failed requests never remove cached data.
- Language selector (English / Spanish) that controls both the in-app UI text and the language of TMDB content.
- Adaptive layout: phones use list → detail navigation; windows at least 840dp wide and 480dp tall show a list/detail two-pane layout.
- Material 3 light and dark themes.
- Each list keeps its own scroll position and, in the two-pane layout, its own selected movie across navigation, rotation and layout changes.

## Tech stack

| Area | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, Navigation Compose (type-safe routes) |
| Presentation | Architecture Components ViewModel, `StateFlow` |
| Dependency injection | Hilt (KSP) |
| Persistence | Room (KSP), with exported schemas in `app/schemas`. Preferences DataStore for the selected language |
| Networking | Retrofit, OkHttp, kotlinx.serialization |
| Images | Coil 3 |
| Async | Kotlin Coroutines and Flow |
| Tests | JUnit 4, kotlinx-coroutines-test, Robolectric, AndroidX Test, Compose UI Test, Room testing |

## Architecture

Clean Architecture with MVVM in a single `app` module. Package root: `dev.alexrincon.movieclient`.

```
core/            Hilt modules for networking (Retrofit, OkHttp, API key) and coroutine dispatchers
domain/          Pure Kotlin: models, MovieError, MovieRepository and language store contracts, use cases
data/
  local/         Room database, migrations, entities, DAOs, entity mappers and merge rules
  language/      DataStore persistence for the selected language
  remote/        Retrofit API, DTOs, TMDB remote data source
  image/         TMDB image URL building
  repository/    MovieRepositoryImpl (offline-first) and error translation
presentation/
  movielist/     List screen, poster grid, MovieListViewModel
  moviedetail/   Detail screen, MovieDetailViewModel
  navigation/    NavHost, routes, adaptive master-detail layout
  language/      Selected-language state and in-app UI localization
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
- A page is requested automatically when the grid reaches its last four items. After a page request fails, it is not retried automatically: the user taps **Retry**, or scrolls away from the end and back.
- Fields missing from a response (poster, backdrop, release date, rating) do not erase cached values.
- Opening a detail refreshes that movie's data without changing list membership.
- On failure, the cache is left unchanged and the UI shows a retryable error, keeping cached content visible.

### Database

Room database version 2. `MIGRATION_1_2` adds the localized `title` column (default `''`) and keeps existing rows; until a movie is refreshed, its original title is shown. There is no destructive migration fallback.

## Language

The **EN/ES** button in the list's top bar selects one language that controls both:

- **UI text**, resolved from `values/` and `values-es/` independently of the Android system locale.
- **TMDB content**, through the `language` request parameter (`en-US` or `es-MX`): localized titles, overviews and posters. Release dates are formatted for the selected language.

The selection is stored with Preferences DataStore. It is read before the first movie request, and nothing is drawn until it is read, so a restart does not flash the other language. A missing or invalid stored value falls back to English.

Changing the language:

1. The new language is saved and the UI switches to it immediately.
2. Page 1 of both lists is requested in the new language. The cache is updated only if both requests succeed.
3. If either request fails, the selected language is **not** reverted. Cached movies stay visible with a retryable error, and Retry or pull-to-refresh reloads the visible list in the selected language. The other list is reloaded when it is opened.

If the selection cannot be saved (a DataStore write error), the language does not change and no refresh starts. The list shows a generic retryable error; Retry and pull-to-refresh reload it in the current language, and the language can be selected again.

Detail requests started in a previous language cannot overwrite data from a newer successful language change. A detail retry still running when the language changes is cancelled, and its result is not shown.

## Adaptive layout

The layout is chosen from the available window size:

| Window | Layout |
|---|---|
| Phone portrait | List → detail navigation |
| Phone landscape (wider than 840dp but less than 480dp tall) | List → detail navigation, so an open detail survives rotation |
| Tablet portrait (for example 800dp wide) | List → detail navigation |
| Tablet landscape or any window ≥ 840dp × 480dp | List and detail side by side |

When a phone-style detail is open and the window grows into two-pane, the movie becomes the selection of the list pane.

## TMDB API setup

1. Create a TMDB account and request an API key (v3 auth) at <https://www.themoviedb.org/settings/api>.
2. Add the key to `local.properties` in the project root, creating the file if needed:

   ```properties
   TMDB_API_KEY=your_key_here
   ```

3. Do not commit `local.properties`. It is already listed in `.gitignore`.

The build injects the key into `BuildConfig.TMDB_API_KEY`, which keeps it out of source control. It does **not** make the key secret: any key embedded in an APK can be extracted. For a production app, requests should go through a backend that holds the key.

Debug builds log HTTP traffic with the `api_key` value redacted. Release builds do not include the logging interceptor.

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
./gradlew :app:assembleRelease    # release APK, unsigned
```

### Release build

- No release signing configuration is included, so `assembleRelease` produces `app-release-unsigned.apk`, which cannot be installed until it is signed. To distribute it, add a `signingConfigs` entry that reads its credentials from outside the repository, or sign the APK with `zipalign` and `apksigner`.
- R8 runs for release builds, but its optimization scope is limited to `androidx.**`, `kotlin.**` and `kotlinx.**` (`optimization.packageScope` in `app/build.gradle.kts`). Application classes (`dev.alexrincon.movieclient`) are not shrunk or obfuscated.

## Tests

```bash
./gradlew :app:testDebugUnitTest            # JVM unit tests
./gradlew :app:connectedDebugAndroidTest    # instrumented tests, needs a device or emulator
./gradlew :app:compileDebugKotlin           # quick compile check
```

**Unit tests** (`app/src/test`):

- `MovieListViewModelTest`, `MovieDetailViewModelTest`: ViewModels with a fake repository, including language changes, failures, pagination and cancellation.
- `MovieContentLanguageStateTest`, `DataStoreMovieContentLanguageStoreTest`: language restore and persistence.
- `MovieEntityMapperTest`, `MovieDtoTest`: merge rules and DTO decoding.
- `MovieFailureTranslatorTest`, `TmdbRemoteDataSourceImplTest`, `TmdbImageUrlBuilderTest`: error translation, request parameters and image URLs.
- `AdaptiveLayoutTest`, `MovieListSelectionsTest`: layout decision for phone, tablet and split-screen sizes, and per-list selection.
- `MovieGridLoadMoreTest`: pagination trigger, including no automatic retry after a failed page.
- `MovieTitleTextTest`, `MovieDetailFormattingTest`: title display and date formatting.

**Instrumented tests** (`app/src/androidTest`):

- `MovieRepositoryImplTest`: repository behavior against an in-memory Room database, including failure handling, pagination, language changes and concurrency.
- `MovieDatabaseMigrationTest`: migration from schema 1 to 2 keeps cached movies and adds the localized title.
- `MovieContentLanguageUiTest`: list UI text follows the selected language without changing the system locale, including a restored Spanish selection.
- `MovieDetailLocalizationTest`: detail screen uses the localized title, shows the original title only when it differs, and localizes labels and dates.
- `MovieListSelectionsRestorationTest`: each list's selected movie survives state restoration.
- `MovieGridStatesTest`: independent grid scroll positions and their restoration.
- `MoviePosterGridLoadMoreTest`: pagination triggers, no automatic retry after a failed page, Retry, and scrolling away and back.

## Known limitations

- Each movie stores one title and overview, in the language of its latest successful response. After a failed language refresh, cached text stays in the previous language until a refresh succeeds.
- TMDB can return an empty overview for a language. The detail screen then shows that no overview is available.
- Cached movies are never pruned. The cache only grows as more pages are loaded.
- There is no background sync. Data refreshes when a list is first opened, on pull-to-refresh, on language change, and when a detail is opened.
- When a two-pane window becomes single-pane (for example, a tablet rotated to portrait), the list is shown. The selected movie is kept and shown again when the two-pane layout returns, but it is not opened as a separate detail screen.
- Navigation and layout switching are covered by unit tests of the layout decision and by manual checks, not by an end-to-end navigation UI test.
- Release builds are unsigned, and application classes are not obfuscated (see [Release build](#release-build)).
