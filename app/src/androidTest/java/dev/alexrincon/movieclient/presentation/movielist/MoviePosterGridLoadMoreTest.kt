package dev.alexrincon.movieclient.presentation.movielist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoviePosterGridLoadMoreTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val movies = List(6) { index -> poster(index + 1) }

    @Test
    fun loadMoreRunsAgainWhenRefreshEndsWhileTheGridStaysAtTheEnd() {
        var refreshing by mutableStateOf(true)
        var calls = 0
        showGrid(
            isRefreshing = { refreshing },
            onLoadMore = { calls += 1 },
        )

        composeRule.waitForIdle()
        assertEquals(0, calls)

        composeRule.runOnIdle { refreshing = false }
        composeRule.waitForIdle()
        assertEquals(1, calls)

        composeRule.waitForIdle()
        assertEquals(1, calls)
    }

    @Test
    fun loadMoreDoesNotRunAgainWhileAPageIsLoading() {
        var loading by mutableStateOf(true)
        var displayed by mutableStateOf(movies)
        var calls = 0
        showGrid(
            movies = { displayed },
            isLoadingMore = { loading },
            onLoadMore = { calls += 1 },
        )

        composeRule.waitForIdle()
        assertEquals(0, calls)

        composeRule.runOnIdle { displayed = movies + poster(7) }
        composeRule.waitForIdle()
        assertEquals(0, calls)

        composeRule.runOnIdle { loading = false }
        composeRule.waitForIdle()
        assertEquals(1, calls)

        composeRule.waitForIdle()
        assertEquals(1, calls)
    }

    @Test
    fun loadMoreIsNotRepeatedWhenTheRequestChangesNothing() {
        var calls = 0
        showGrid(onLoadMore = { calls += 1 })

        composeRule.waitForIdle()
        assertEquals(1, calls)

        composeRule.waitForIdle()
        assertEquals(1, calls)
    }

    private fun showGrid(
        movies: () -> List<Movie> = { this.movies },
        isLoadingMore: () -> Boolean = { false },
        isRefreshing: () -> Boolean = { false },
        onLoadMore: () -> Unit,
    ) {
        composeRule.setContent {
            val gridState = remember { LazyGridState() }
            MovieClientTheme {
                Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                    MoviePosterGrid(
                        movies = movies(),
                        gridState = gridState,
                        isLoadingMore = isLoadingMore(),
                        isRefreshing = isRefreshing(),
                        onMovieClick = {},
                        onLoadMore = onLoadMore,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun poster(id: Int): Movie {
        return Movie(
            id = id,
            originalTitle = "Movie $id",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            voteAverage = 7.0,
        )
    }
}
