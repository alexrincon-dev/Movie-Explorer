package dev.alexrincon.movieclient.presentation.moviedetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.presentation.language.ProvideMovieContentLanguage
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieDetailLocalizationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailUsesLocalizedTitleAndFollowsContentLanguage() {
        val systemLocale = systemLocaleSnapshot()
        var language by mutableStateOf(MovieContentLanguage.EN_US)
        var movie by mutableStateOf(differentTitles())
        var phase by mutableStateOf(DetailPhase.Content)
        composeRule.setContent {
            MovieClientTheme {
                ProvideMovieContentLanguage(language) {
                    MovieDetailContent(
                        uiState = when (phase) {
                            DetailPhase.Content -> MovieDetailUiState(movie = movie, isLoading = false)
                            DetailPhase.Unavailable -> MovieDetailUiState(isLoading = false)
                            DetailPhase.Loading -> MovieDetailUiState(isLoading = true)
                        },
                        onBack = {},
                        onRetry = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("The Other Mother", substring = false).assertIsDisplayed()
        composeRule.onAllNodesWithText("Other Mommy", substring = false).assertCountEquals(1)
        composeRule.onNodeWithText("Original title", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Year", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Rating", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Release date", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Overview", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Jul 16, 2010", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("8.4", substring = false).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Go back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Poster for The Other Mother").assertIsDisplayed()

        composeRule.runOnIdle { movie = sameTitles() }
        composeRule.onNodeWithText("Inception", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Original title", substring = false).assertDoesNotExist()

        composeRule.runOnIdle { movie = blankLocalizedTitle() }
        composeRule.onNodeWithText("Other Mommy", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Original title", substring = false).assertDoesNotExist()

        composeRule.runOnIdle {
            movie = differentTitles()
            language = MovieContentLanguage.ES_MX
        }
        composeRule.onNodeWithText("The Other Mother", substring = false).assertIsDisplayed()
        composeRule.onAllNodesWithText("Other Mommy", substring = false).assertCountEquals(1)
        composeRule.onNodeWithText("Título original", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Año", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Valoración", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Fecha de estreno", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Sinopsis", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("A summary", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Jul 16, 2010", substring = false).assertDoesNotExist()
        composeRule.onNodeWithText("2010", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Year", substring = false).assertDoesNotExist()
        composeRule.onNodeWithText("Original title", substring = false).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Póster de The Other Mother").assertIsDisplayed()

        composeRule.runOnIdle { phase = DetailPhase.Unavailable }
        composeRule.onNodeWithText("Esta película no está disponible.", substring = false).assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar", substring = false).assertIsDisplayed()

        composeRule.runOnIdle { phase = DetailPhase.Loading }
        composeRule.onNodeWithContentDescription("Cargando película").assertIsDisplayed()
        assertEquals(systemLocale, systemLocaleSnapshot())
    }

    private fun systemLocaleSnapshot(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.resources.configuration.locales.toLanguageTags() +
            "|" + Locale.getDefault().toLanguageTag()
    }

    private fun differentTitles(): Movie = movie(title = "The Other Mother", originalTitle = "Other Mommy")

    private fun sameTitles(): Movie = movie(title = "Inception", originalTitle = "Inception")

    private fun blankLocalizedTitle(): Movie = movie(title = " ", originalTitle = "Other Mommy")

    private fun movie(title: String, originalTitle: String): Movie {
        return Movie(
            id = 1,
            title = title,
            originalTitle = originalTitle,
            overview = "A summary",
            posterPath = null,
            backdropPath = null,
            releaseDate = "2010-07-16",
            voteAverage = 8.4,
        )
    }

    private enum class DetailPhase {
        Content,
        Unavailable,
        Loading,
    }
}
