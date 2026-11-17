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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.presentation.language.ProvideMovieContentLanguage
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme
import kotlinx.coroutines.runBlocking
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

    @Test
    fun failedPageIsNotRequestedAgainWhileTheGridStaysAtTheEnd() {
        val page = FakePageRequests()
        showGrid(
            isLoadingMore = { page.loading },
            paginationFailed = { page.failed },
            onLoadMore = page::start,
        )

        composeRule.waitForIdle()
        assertEquals(1, page.calls)

        page.fail()
        composeRule.waitForIdle()
        assertEquals(1, page.calls)

        composeRule.waitForIdle()
        assertEquals(1, page.calls)
    }

    @Test
    fun failedPageIsRequestedAgainAfterScrollingAwayAndBack() {
        val manyMovies = List(30) { index -> poster(index + 1) }
        val gridState = LazyGridState()
        val page = FakePageRequests()
        showGrid(
            movies = { manyMovies },
            gridState = gridState,
            isLoadingMore = { page.loading },
            paginationFailed = { page.failed },
            onLoadMore = page::start,
        )
        composeRule.waitForIdle()
        assertEquals(0, page.calls)

        scrollTo(gridState, manyMovies.lastIndex)
        assertEquals(1, page.calls)
        page.fail()
        composeRule.waitForIdle()
        assertEquals(1, page.calls)

        scrollTo(gridState, 0)
        assertEquals(1, page.calls)
        scrollTo(gridState, manyMovies.lastIndex)
        assertEquals(2, page.calls)

        page.fail()
        composeRule.waitForIdle()
        assertEquals(2, page.calls)
    }

    @Test
    fun refreshEndingAtTheEndRequestsAPageAfterAFailure() {
        var refreshing by mutableStateOf(false)
        val page = FakePageRequests()
        showGrid(
            isLoadingMore = { page.loading },
            isRefreshing = { refreshing },
            paginationFailed = { page.failed },
            onLoadMore = page::start,
        )
        composeRule.waitForIdle()
        page.fail()
        composeRule.waitForIdle()
        assertEquals(1, page.calls)

        composeRule.runOnIdle {
            refreshing = true
            page.failed = false
        }
        composeRule.waitForIdle()
        assertEquals(1, page.calls)

        composeRule.runOnIdle { refreshing = false }
        composeRule.waitForIdle()
        assertEquals(2, page.calls)
    }

    @Test
    fun retryAfterAFailedPageRequestsItExactlyOnce() {
        val page = FakePageRequests()
        composeRule.setContent {
            MovieClientTheme {
                ProvideMovieContentLanguage(MovieContentLanguage.EN_US) {
                    Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                        MovieListContent(
                            uiState = MovieListUiState(
                                movies = movies,
                                contentLanguage = MovieContentLanguage.EN_US,
                                isInitialLoading = false,
                                isLoadingMore = page.loading,
                                canLoadMore = true,
                                error = if (page.failed) MovieError.RateLimited else null,
                                isPaginationError = page.failed,
                            ),
                            gridStates = rememberMovieGridStates(),
                            onMovieClick = {},
                            onListTypeSelected = {},
                            onRefresh = {},
                            onLoadMore = page::start,
                            onLanguageSelected = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(1, page.calls)
        page.fail()
        composeRule.waitForIdle()
        assertEquals(1, page.calls)

        composeRule.onNodeWithText("Retry", substring = false).performClick()
        composeRule.waitForIdle()
        assertEquals(2, page.calls)

        page.fail()
        composeRule.waitForIdle()
        assertEquals(2, page.calls)
    }

    /**
     * Mirrors the ViewModel state changes for one page request.
     * A request starts loading and clears the previous failure. A slow failure is shown while
     * loading, and loading ends in a later frame.
     */
    private inner class FakePageRequests {
        var loading by mutableStateOf(false)
        var failed by mutableStateOf(false)
        var calls = 0

        fun start() {
            calls += 1
            loading = true
            failed = false
        }

        fun fail() {
            composeRule.runOnIdle { failed = true }
            composeRule.waitForIdle()
            composeRule.runOnIdle { loading = false }
        }
    }

    private fun scrollTo(gridState: LazyGridState, index: Int) {
        composeRule.runOnIdle {
            runBlocking { gridState.scrollToItem(index) }
        }
        composeRule.waitForIdle()
    }

    private fun showGrid(
        movies: () -> List<Movie> = { this.movies },
        gridState: LazyGridState? = null,
        isLoadingMore: () -> Boolean = { false },
        isRefreshing: () -> Boolean = { false },
        paginationFailed: () -> Boolean = { false },
        onLoadMore: () -> Unit,
    ) {
        composeRule.setContent {
            val state = gridState ?: remember { LazyGridState() }
            MovieClientTheme {
                Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                    MoviePosterGrid(
                        movies = movies(),
                        gridState = state,
                        isLoadingMore = isLoadingMore(),
                        isRefreshing = isRefreshing(),
                        paginationFailed = paginationFailed(),
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
            title = "Movie $id",
            originalTitle = "Movie $id",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            voteAverage = 7.0,
        )
    }
}
