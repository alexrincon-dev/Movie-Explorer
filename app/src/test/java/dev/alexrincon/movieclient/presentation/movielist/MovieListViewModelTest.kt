@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.alexrincon.movieclient.presentation.movielist

import androidx.lifecycle.viewModelScope
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.domain.usecase.LoadMoreMoviesUseCase
import dev.alexrincon.movieclient.domain.usecase.RefreshMoviesUseCase
import dev.alexrincon.movieclient.presentation.FakeMovieRepository
import dev.alexrincon.movieclient.presentation.ListRequest
import dev.alexrincon.movieclient.presentation.language.MovieContentLanguageState
import dev.alexrincon.movieclient.presentation.runViewModelTest
import dev.alexrincon.movieclient.presentation.testMovie
import dev.alexrincon.movieclient.presentation.testSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieListViewModelTest {
    private val repository = FakeMovieRepository()
    private val languageState = MovieContentLanguageState()

    @Test
    fun startsOnPopularAndSwitchesTheObservedListWithoutRepeatingRefresh() = runViewModelTest {
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(listOf(testMovie(1)))
        repository.listSnapshots.getValue(MovieListType.TOP_RATED).value =
            testSnapshot(listOf(testMovie(2)))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(MovieListType.POPULAR, viewModel.uiState.value.selectedListType)
        assertEquals(listOf(testMovie(1)), viewModel.uiState.value.movies)
        assertEquals(1, repository.activePopularCollections)
        assertEquals(
            listOf(ListRequest(MovieListType.POPULAR, MovieContentLanguage.EN_US)),
            repository.refreshCalls,
        )

        viewModel.selectListType(MovieListType.POPULAR)
        advanceUntilIdle()
        assertEquals(1, repository.refreshCalls.size)

        viewModel.selectListType(MovieListType.TOP_RATED)
        advanceUntilIdle()

        assertEquals(MovieListType.TOP_RATED, viewModel.uiState.value.selectedListType)
        assertEquals(listOf(testMovie(2)), viewModel.uiState.value.movies)
        assertEquals(0, repository.activePopularCollections)
        assertEquals(
            listOf(
                ListRequest(MovieListType.POPULAR, MovieContentLanguage.EN_US),
                ListRequest(MovieListType.TOP_RATED, MovieContentLanguage.EN_US),
            ),
            repository.refreshCalls,
        )

        viewModel.selectListType(MovieListType.POPULAR)
        viewModel.selectListType(MovieListType.TOP_RATED)
        advanceUntilIdle()
        assertEquals(2, repository.refreshCalls.size)
        assertEquals(listOf(testMovie(2)), viewModel.uiState.value.movies)
    }

    @Test
    fun keepsCachedMoviesVisibleWhileRefreshing() = runViewModelTest {
        val cached = listOf(testMovie(9))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value = testSnapshot(cached)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.refreshGate = CompletableDeferred()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(cached, viewModel.uiState.value.movies)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun loadMoreIgnoresDuplicateTriggersAndKeepsCachedMovies() = runViewModelTest {
        val cached = listOf(testMovie(1))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(cached, lastLoadedPage = 1, totalPages = 3)
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canLoadMore)

        repository.loadMoreGate = CompletableDeferred()
        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(
            listOf(ListRequest(MovieListType.POPULAR, MovieContentLanguage.EN_US)),
            repository.loadMoreCalls,
        )
        assertTrue(viewModel.uiState.value.isLoadingMore)
        assertEquals(cached, viewModel.uiState.value.movies)
    }

    @Test
    fun loadMoreDoesNothingWhenAnotherPageIsUnavailable() = runViewModelTest {
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(listOf(testMovie(1)), lastLoadedPage = 1, totalPages = 1)
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canLoadMore)
        viewModel.loadMore()
        advanceUntilIdle()

        assertTrue(repository.loadMoreCalls.isEmpty())
    }

    @Test
    fun languageChangeRefreshesBothListsAndPublishesLanguageOnlyAfterSuccess() = runViewModelTest {
        val cached = listOf(testMovie(4))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value = testSnapshot(cached)
        val viewModel = viewModel()
        advanceUntilIdle()
        val refreshesBeforeLanguageChange = repository.refreshCalls.size

        viewModel.changeContentLanguage(MovieContentLanguage.EN_US)
        advanceUntilIdle()
        assertTrue(repository.refreshAllCalls.isEmpty())

        repository.refreshAllGate = CompletableDeferred()
        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(listOf(MovieContentLanguage.ES_MX), repository.refreshAllCalls)
        assertEquals(MovieContentLanguage.EN_US, languageState.contentLanguage.value)
        assertEquals(MovieContentLanguage.EN_US, viewModel.uiState.value.contentLanguage)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertTrue(viewModel.uiState.value.isRefreshing)

        repository.refreshAllGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(MovieContentLanguage.ES_MX, languageState.contentLanguage.value)
        assertEquals(MovieContentLanguage.ES_MX, viewModel.uiState.value.contentLanguage)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertEquals(refreshesBeforeLanguageChange, repository.refreshCalls.size)

        viewModel.selectListType(MovieListType.TOP_RATED)
        advanceUntilIdle()
        assertEquals(refreshesBeforeLanguageChange, repository.refreshCalls.size)
    }

    @Test
    fun failedLanguageChangeKeepsThePreviousLanguage() = runViewModelTest {
        val cached = listOf(testMovie(5))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value = testSnapshot(cached)
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.refreshAllError = MovieException(MovieError.Network)

        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(listOf(MovieContentLanguage.ES_MX), repository.refreshAllCalls)
        assertEquals(MovieContentLanguage.EN_US, languageState.contentLanguage.value)
        assertEquals(MovieContentLanguage.EN_US, viewModel.uiState.value.contentLanguage)
        assertEquals(MovieError.Network, viewModel.uiState.value.error)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertFalse(viewModel.uiState.value.isRefreshing)

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(MovieContentLanguage.EN_US, repository.refreshCalls.last().language)
    }

    @Test
    fun refreshFailureKeepsCachedMoviesAndExposesMovieError() = runViewModelTest {
        val cached = listOf(testMovie(3))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(cached, lastLoadedPage = 1, totalPages = 3)
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.refreshError = MovieException(MovieError.Server)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(MovieError.Server, viewModel.uiState.value.error)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertTrue(viewModel.uiState.value.canLoadMore)
        assertFalse(viewModel.uiState.value.isPaginationError)
        assertFalse(viewModel.uiState.value.retriesFailedPage)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.isInitialLoading)
    }

    @Test
    fun loadMoreFailureKeepsCachedMoviesAndExposesMovieError() = runViewModelTest {
        val cached = listOf(testMovie(6))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(cached, lastLoadedPage = 1, totalPages = 4)
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.loadMoreError = MovieException(MovieError.RateLimited)

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(MovieError.RateLimited, viewModel.uiState.value.error)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertTrue(viewModel.uiState.value.isPaginationError)
        assertTrue(viewModel.uiState.value.retriesFailedPage)

        val refreshesAfterFailure = repository.refreshCalls.size
        repository.loadMoreError = null
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(refreshesAfterFailure, repository.refreshCalls.size)
        assertEquals(2, repository.loadMoreCalls.size)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isPaginationError)
        assertEquals(cached, viewModel.uiState.value.movies)
    }

    @Test
    fun initialLoadFailureRetriesAsRefresh() = runViewModelTest {
        repository.refreshError = MovieException(MovieError.Network)
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.movies.isEmpty())
        assertEquals(MovieError.Network, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isInitialLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.isPaginationError)
        assertFalse(viewModel.uiState.value.retriesFailedPage)
        assertTrue(repository.loadMoreCalls.isEmpty())
    }

    @Test
    fun retryChoiceStaysWithRefreshUnlessTheVisibleErrorCameFromPagination() {
        val cached = listOf(testMovie(1))
        assertFalse(
            MovieListUiState(
                error = MovieError.Network,
            ).retriesFailedPage,
        )
        assertFalse(
            MovieListUiState(
                movies = cached,
                canLoadMore = true,
                error = MovieError.Server,
            ).retriesFailedPage,
        )
        assertTrue(
            MovieListUiState(
                movies = cached,
                canLoadMore = true,
                error = MovieError.RateLimited,
                isPaginationError = true,
            ).retriesFailedPage,
        )
    }

    @Test
    fun cancelledRefreshDoesNotBecomeMovieError() = runViewModelTest {
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(listOf(testMovie(7)))
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.refreshGate = CompletableDeferred()

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.viewModelScope.cancel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(listOf(testMovie(7)), viewModel.uiState.value.movies)
    }

    private fun viewModel(): MovieListViewModel {
        return MovieListViewModel(
            movieRepository = repository,
            refreshMovies = RefreshMoviesUseCase(repository),
            loadMoreMovies = LoadMoreMoviesUseCase(repository),
            contentLanguageState = languageState,
        )
    }
}
