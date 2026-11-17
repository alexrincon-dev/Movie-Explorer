@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.alexrincon.movieclient.presentation.movielist

import androidx.lifecycle.viewModelScope
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.domain.usecase.LoadMoreMoviesUseCase
import dev.alexrincon.movieclient.domain.usecase.RefreshMoviesUseCase
import dev.alexrincon.movieclient.presentation.FakeMovieContentLanguageStore
import dev.alexrincon.movieclient.presentation.FakeMovieRepository
import dev.alexrincon.movieclient.presentation.ListRequest
import dev.alexrincon.movieclient.presentation.language.MovieContentLanguageState
import dev.alexrincon.movieclient.presentation.runViewModelTest
import dev.alexrincon.movieclient.presentation.testMovie
import dev.alexrincon.movieclient.presentation.testSnapshot
import java.io.IOException
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
    private val languageStore = FakeMovieContentLanguageStore()
    private val languageState = MovieContentLanguageState(languageStore)

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
    fun languageChangePersistsImmediatelyAndRefreshesBothListsOnce() = runViewModelTest {
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
        viewModel.changeContentLanguage(MovieContentLanguage.EN_US)
        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(listOf(MovieContentLanguage.ES_MX), repository.refreshAllCalls)
        assertEquals(MovieContentLanguage.ES_MX, languageState.contentLanguage.value)
        assertEquals(MovieContentLanguage.ES_MX, viewModel.uiState.value.contentLanguage)
        assertEquals(listOf(MovieContentLanguage.ES_MX), languageStore.writes)
        assertEquals(MovieContentLanguage.ES_MX, languageStore.stored)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)

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
    fun initialRefreshUsesTheRestoredLanguageAndDoesNotStartBeforeItLoads() = runViewModelTest {
        languageStore.stored = MovieContentLanguage.ES_MX
        val viewModel = viewModel()

        assertTrue(repository.refreshCalls.isEmpty())
        assertNull(viewModel.uiState.value.contentLanguage)

        advanceUntilIdle()

        assertEquals(
            listOf(ListRequest(MovieListType.POPULAR, MovieContentLanguage.ES_MX)),
            repository.refreshCalls,
        )
        assertEquals(MovieContentLanguage.ES_MX, viewModel.uiState.value.contentLanguage)
        assertTrue(languageStore.writes.isEmpty())
    }

    @Test
    fun loadMoreDuringRefreshIsIgnoredAndWorksAfterTheRefreshCompletes() = runViewModelTest {
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(listOf(testMovie(1)), lastLoadedPage = 1, totalPages = 3)
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.refreshGate = CompletableDeferred()

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertTrue(repository.loadMoreCalls.isEmpty())
        assertTrue(viewModel.uiState.value.isRefreshing)

        repository.refreshGate?.complete(Unit)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(
            listOf(ListRequest(MovieListType.POPULAR, MovieContentLanguage.EN_US)),
            repository.loadMoreCalls,
        )
    }

    @Test
    fun failedLanguageChangeKeepsSpanishAndTheCachedMovies() = runViewModelTest {
        val cached = listOf(testMovie(5))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(cached, lastLoadedPage = 1, totalPages = 3)
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.selectListType(MovieListType.TOP_RATED)
        advanceUntilIdle()
        viewModel.selectListType(MovieListType.POPULAR)
        advanceUntilIdle()
        repository.refreshAllError = MovieException(MovieError.Network)

        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(listOf(MovieContentLanguage.ES_MX), repository.refreshAllCalls)
        assertEquals(MovieContentLanguage.ES_MX, languageState.contentLanguage.value)
        assertEquals(MovieContentLanguage.ES_MX, viewModel.uiState.value.contentLanguage)
        assertEquals(listOf(MovieContentLanguage.ES_MX), languageStore.writes)
        assertEquals(MovieContentLanguage.ES_MX, languageStore.stored)
        assertEquals(MovieError.Network, viewModel.uiState.value.error)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.isInitialLoading)

        val restored = MovieContentLanguageState(languageStore)
        assertEquals(MovieContentLanguage.ES_MX, restored.awaitLanguage())

        viewModel.loadMore()
        advanceUntilIdle()
        assertTrue(repository.loadMoreCalls.isEmpty())

        viewModel.selectListType(MovieListType.TOP_RATED)
        advanceUntilIdle()
        assertEquals(
            ListRequest(MovieListType.TOP_RATED, MovieContentLanguage.ES_MX),
            repository.refreshCalls.last(),
        )

        val localized = listOf(
            testMovie(5).copy(title = "Película 5", overview = "Sinopsis 5"),
        )
        repository.refreshAllError = null
        repository.listSnapshots.getValue(MovieListType.POPULAR).value = testSnapshot(localized)
        viewModel.selectListType(MovieListType.POPULAR)
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(MovieContentLanguage.ES_MX, repository.refreshCalls.last().language)
        assertEquals(localized, viewModel.uiState.value.movies)
        assertNull(viewModel.uiState.value.error)
        assertEquals(MovieContentLanguage.ES_MX, languageStore.stored)
    }

    @Test
    fun failedLanguageChangeDoesNotLetAnOlderRefreshRevertTheSelection() = runViewModelTest {
        val cached = listOf(testMovie(8))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value = testSnapshot(cached)
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.refreshGate = CompletableDeferred()
        viewModel.refresh()
        advanceUntilIdle()
        val refreshesWhileOldRequestIsRunning = repository.refreshCalls.size

        repository.refreshAllError = MovieException(MovieError.Network)
        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()
        repository.refreshGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(MovieContentLanguage.ES_MX, languageState.contentLanguage.value)
        assertEquals(MovieContentLanguage.ES_MX, languageStore.stored)
        assertEquals(MovieError.Network, viewModel.uiState.value.error)
        assertEquals(cached, viewModel.uiState.value.movies)
        assertEquals(refreshesWhileOldRequestIsRunning, repository.refreshCalls.size)
        assertTrue(
            repository.refreshCalls.none { request ->
                request.language == MovieContentLanguage.ES_MX
            },
        )
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
    fun slowPageFailureEndsLoadingAndRetryRequestsTheFailedPageOnce() = runViewModelTest {
        val cached = listOf(testMovie(6))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(cached, lastLoadedPage = 1, totalPages = 4)
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.loadMoreGate = CompletableDeferred()

        viewModel.loadMore()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoadingMore)
        assertFalse(viewModel.uiState.value.isPaginationError)

        repository.loadMoreError = MovieException(MovieError.RateLimited)
        repository.loadMoreGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.loadMoreCalls.size)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertTrue(viewModel.uiState.value.isPaginationError)
        assertTrue(viewModel.uiState.value.retriesFailedPage)

        repository.loadMoreGate = CompletableDeferred()
        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(2, repository.loadMoreCalls.size)
        assertTrue(viewModel.uiState.value.isLoadingMore)
        assertFalse(viewModel.uiState.value.isPaginationError)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun loadMoreStopsOnceTheLastPageIsCached() = runViewModelTest {
        val snapshots = repository.listSnapshots.getValue(MovieListType.POPULAR)
        snapshots.value = testSnapshot(listOf(testMovie(1)), lastLoadedPage = 1, totalPages = 2)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()
        snapshots.value = testSnapshot(
            listOf(testMovie(1), testMovie(2)),
            lastLoadedPage = 2,
            totalPages = 2,
        )
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(1, repository.loadMoreCalls.size)
        assertFalse(viewModel.uiState.value.canLoadMore)
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

    @Test
    fun languageSaveFailureKeepsTheLanguageAndShowsAnError() = runViewModelTest {
        val cached = listOf(testMovie(4))
        repository.listSnapshots.getValue(MovieListType.POPULAR).value = testSnapshot(cached)
        val viewModel = viewModel()
        advanceUntilIdle()
        val refreshesBeforeLanguageChange = repository.refreshCalls.size
        languageStore.writeError = IOException("disk full")

        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(MovieContentLanguage.EN_US, languageState.contentLanguage.value)
        assertEquals(MovieContentLanguage.EN_US, viewModel.uiState.value.contentLanguage)
        assertEquals(MovieContentLanguage.EN_US, languageStore.stored)
        assertTrue(languageStore.writes.isEmpty())
        assertTrue(repository.refreshAllCalls.isEmpty())
        assertEquals(refreshesBeforeLanguageChange, repository.refreshCalls.size)
        assertEquals(MovieError.Unknown, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isPaginationError)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(cached, viewModel.uiState.value.movies)
    }

    @Test
    fun afterALanguageSaveFailureRefreshAndLanguageChangeStillWork() = runViewModelTest {
        repository.listSnapshots.getValue(MovieListType.POPULAR).value =
            testSnapshot(listOf(testMovie(4)))
        val viewModel = viewModel()
        advanceUntilIdle()
        languageStore.writeError = IOException("disk full")
        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            ListRequest(MovieListType.POPULAR, MovieContentLanguage.EN_US),
            repository.refreshCalls.last(),
        )
        assertNull(viewModel.uiState.value.error)

        languageStore.writeError = null
        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(listOf(MovieContentLanguage.ES_MX), languageStore.writes)
        assertEquals(listOf(MovieContentLanguage.ES_MX), repository.refreshAllCalls)
        assertEquals(MovieContentLanguage.ES_MX, viewModel.uiState.value.contentLanguage)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun cancelledLanguageSaveDoesNotBecomeAnError() = runViewModelTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        languageStore.writeGate = CompletableDeferred()

        viewModel.changeContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRefreshing)

        viewModel.viewModelScope.cancel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(MovieContentLanguage.EN_US, languageState.contentLanguage.value)
        assertTrue(languageStore.writes.isEmpty())
        assertTrue(repository.refreshAllCalls.isEmpty())
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
