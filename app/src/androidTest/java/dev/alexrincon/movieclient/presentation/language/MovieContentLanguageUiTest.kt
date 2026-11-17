package dev.alexrincon.movieclient.presentation.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.alexrincon.movieclient.data.language.DataStoreMovieContentLanguageStore
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.presentation.movielist.MovieListContent
import dev.alexrincon.movieclient.presentation.movielist.MovieListUiState
import dev.alexrincon.movieclient.presentation.movielist.rememberMovieGridStates
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieContentLanguageUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listLabelsFollowContentLanguageWhileTheSystemLocaleStays() {
        val systemLocale = systemLocaleSnapshot()
        var language by mutableStateOf(MovieContentLanguage.EN_US)
        var phase by mutableStateOf(ListPhase.Content)
        composeRule.setContent {
            MovieClientTheme {
                ProvideMovieContentLanguage(language) {
                    MovieListContent(
                        uiState = listState(language, phase),
                        gridStates = rememberMovieGridStates(),
                        onMovieClick = {},
                        onListTypeSelected = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onLanguageSelected = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Popular", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Top Rated", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Retry", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText(ENGLISH_NETWORK_ERROR, substring = false).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Poster for La otra madre").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Content language: English").assertIsDisplayed()

        composeRule.runOnIdle { phase = ListPhase.Empty }
        composeRule.onNodeWithText("No movies to show.", substring = false).assertIsDisplayed()

        composeRule.runOnIdle { phase = ListPhase.Loading }
        composeRule.onNodeWithContentDescription("Loading movies").assertIsDisplayed()

        composeRule.runOnIdle {
            phase = ListPhase.Content
            language = MovieContentLanguage.ES_MX
        }
        composeRule.onNodeWithText("Populares", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Mejor valoradas", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText(SPANISH_NETWORK_ERROR, substring = false).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Póster de La otra madre").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Idioma del contenido: Español").assertIsDisplayed()
        composeRule.onNodeWithText("Popular", substring = false).assertDoesNotExist()
        composeRule.onNodeWithText("Retry", substring = false).assertDoesNotExist()
        assertEquals(systemLocale, systemLocaleSnapshot())
    }

    @Test
    fun persistedSpanishSelectionRestoresSpanishUi() {
        val systemLocale = systemLocaleSnapshot()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "language-ui-${System.nanoTime()}").apply { mkdirs() }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { File(directory, "movie_content_language.preferences_pb") },
        )
        val store = DataStoreMovieContentLanguageStore(dataStore)
        val restored = MovieContentLanguageState(store)
        runBlocking {
            store.write(MovieContentLanguage.ES_MX)
            assertEquals(MovieContentLanguage.ES_MX, restored.awaitLanguage())
        }

        composeRule.setContent {
            MovieClientTheme {
                MovieContentLanguageHost(restored) {
                    MovieListContent(
                        uiState = listState(MovieContentLanguage.ES_MX, ListPhase.Empty),
                        gridStates = rememberMovieGridStates(),
                        onMovieClick = {},
                        onListTypeSelected = {},
                        onRefresh = {},
                        onLoadMore = {},
                        onLanguageSelected = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Populares", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Mejor valoradas", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("No hay películas para mostrar.", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Popular", substring = false).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Idioma del contenido: Español").assertIsDisplayed()
        assertEquals(systemLocale, systemLocaleSnapshot())
    }

    private fun systemLocaleSnapshot(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.resources.configuration.locales.toLanguageTags() +
            "|" + Locale.getDefault().toLanguageTag()
    }

    private fun listState(language: MovieContentLanguage, phase: ListPhase): MovieListUiState {
        return when (phase) {
            ListPhase.Content -> MovieListUiState(
                movies = listOf(localizedMovie()),
                contentLanguage = language,
                isInitialLoading = false,
                error = MovieError.Network,
            )
            ListPhase.Empty -> MovieListUiState(
                contentLanguage = language,
                isInitialLoading = false,
            )
            ListPhase.Loading -> MovieListUiState(
                contentLanguage = language,
                isInitialLoading = true,
            )
        }
    }

    private fun localizedMovie(): Movie {
        return Movie(
            id = 1,
            title = "La otra madre",
            originalTitle = "Other Mommy",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            voteAverage = 7.0,
        )
    }

    private enum class ListPhase {
        Content,
        Empty,
        Loading,
    }

    private companion object {
        const val ENGLISH_NETWORK_ERROR =
            "No internet connection. Check your connection and try again."
        const val SPANISH_NETWORK_ERROR =
            "Sin conexión a internet. Comprueba tu conexión e inténtalo de nuevo."
    }
}
