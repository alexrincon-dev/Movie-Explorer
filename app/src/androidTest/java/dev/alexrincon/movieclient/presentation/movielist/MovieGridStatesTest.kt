package dev.alexrincon.movieclient.presentation.movielist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieGridStatesTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val movies = List(100) { index ->
        Movie(
            id = index + 1,
            title = "Movie ${index + 1}",
            originalTitle = "Movie ${index + 1}",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            voteAverage = 7.0,
        )
    }

    @Test
    fun scrollPositionSurvivesSavedStateRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var gridStates: MovieGridStates
        restorationTester.setContent {
            gridStates = rememberMovieGridStates()
            Grid(gridStates, MovieListType.POPULAR)
        }
        scrollTo(gridStates, MovieListType.POPULAR, index = 40)
        val beforeRestore = gridStates

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle {
            assertNotSame(beforeRestore, gridStates)
            assertEquals(40, gridStates[MovieListType.POPULAR].firstVisibleItemIndex)
        }
    }

    @Test
    fun popularAndTopRatedKeepIndependentPositions() {
        var listType by mutableStateOf(MovieListType.POPULAR)
        lateinit var gridStates: MovieGridStates
        composeRule.setContent {
            gridStates = rememberMovieGridStates()
            Grid(gridStates, listType)
        }
        scrollTo(gridStates, MovieListType.POPULAR, index = 40)

        composeRule.runOnIdle { listType = MovieListType.TOP_RATED }
        composeRule.runOnIdle {
            assertEquals(0, gridStates[MovieListType.TOP_RATED].firstVisibleItemIndex)
        }
        scrollTo(gridStates, MovieListType.TOP_RATED, index = 10)

        composeRule.runOnIdle { listType = MovieListType.POPULAR }
        composeRule.runOnIdle {
            assertEquals(40, gridStates[MovieListType.POPULAR].firstVisibleItemIndex)
            assertEquals(10, gridStates[MovieListType.TOP_RATED].firstVisibleItemIndex)
        }
    }

    @Test
    fun scrollPositionSurvivesTheGridLeavingComposition() {
        var showGrid by mutableStateOf(true)
        lateinit var gridStates: MovieGridStates
        composeRule.setContent {
            gridStates = rememberMovieGridStates()
            if (showGrid) {
                Grid(gridStates, MovieListType.POPULAR)
            }
        }
        scrollTo(gridStates, MovieListType.POPULAR, index = 40)

        composeRule.runOnIdle { showGrid = false }
        composeRule.runOnIdle { showGrid = true }

        composeRule.runOnIdle {
            assertEquals(40, gridStates[MovieListType.POPULAR].firstVisibleItemIndex)
        }
    }

    @Composable
    private fun Grid(gridStates: MovieGridStates, listType: MovieListType) {
        MovieClientTheme {
            MoviePosterGrid(
                movies = movies,
                gridState = gridStates[listType],
                isLoadingMore = false,
                onMovieClick = {},
                onLoadMore = {},
            )
        }
    }

    private fun scrollTo(gridStates: MovieGridStates, listType: MovieListType, index: Int) {
        composeRule.runOnIdle {
            runBlocking { gridStates[listType].scrollToItem(index) }
        }
        composeRule.runOnIdle {
            assertEquals(index, gridStates[listType].firstVisibleItemIndex)
        }
    }
}
