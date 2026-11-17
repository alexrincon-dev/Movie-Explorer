@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.alexrincon.movieclient.presentation.moviedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.presentation.DetailRequest
import dev.alexrincon.movieclient.presentation.FakeMovieContentLanguageStore
import dev.alexrincon.movieclient.presentation.FakeMovieRepository
import dev.alexrincon.movieclient.presentation.language.MovieContentLanguageState
import dev.alexrincon.movieclient.presentation.runViewModelTest
import dev.alexrincon.movieclient.presentation.testMovie
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MovieDetailViewModelTest {
    private val repository = FakeMovieRepository()
    private val languageState = MovieContentLanguageState(FakeMovieContentLanguageStore())

    @Test
    fun observesRequestedMovieAndRefreshesWithCurrentLanguage() = runViewModelTest {
        languageState.updateContentLanguage(MovieContentLanguage.ES_MX)
        val viewModel = viewModel(movieId = 21)

        advanceUntilIdle()

        assertEquals(listOf(21), repository.observedMovieIds)
        assertEquals(
            listOf(DetailRequest(movieId = 21, language = MovieContentLanguage.ES_MX)),
            repository.detailRefreshCalls,
        )
        assertNull(viewModel.uiState.value.movie)

        languageState.updateContentLanguage(MovieContentLanguage.EN_US)
        advanceUntilIdle()

        assertEquals(
            listOf(
                DetailRequest(movieId = 21, language = MovieContentLanguage.ES_MX),
                DetailRequest(movieId = 21, language = MovieContentLanguage.EN_US),
            ),
            repository.detailRefreshCalls,
        )
        assertEquals(listOf(21), repository.observedMovieIds)
    }

    @Test
    fun keepsCachedMovieVisibleWhileRefreshing() = runViewModelTest {
        val cached = testMovie(8)
        repository.movieSnapshots[8] = MutableStateFlow(cached)
        repository.detailGate = CompletableDeferred()
        val viewModel = viewModel(movieId = 8)

        advanceUntilIdle()

        assertEquals(cached, viewModel.uiState.value.movie)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(
            listOf(DetailRequest(movieId = 8, language = MovieContentLanguage.EN_US)),
            repository.detailRefreshCalls,
        )
    }

    @Test
    fun refreshFailureExposesMovieErrorAndKeepsCachedMovie() = runViewModelTest {
        val cached = testMovie(8)
        repository.movieSnapshots[8] = MutableStateFlow(cached)
        repository.detailError = MovieException(MovieError.NotFound)
        val viewModel = viewModel(movieId = 8)

        advanceUntilIdle()

        assertEquals(cached, viewModel.uiState.value.movie)
        assertEquals(MovieError.NotFound, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun retryRefreshesTheSameMovieAfterFailure() = runViewModelTest {
        repository.detailError = MovieException(MovieError.Network)
        val viewModel = viewModel(movieId = 4)

        advanceUntilIdle()

        assertEquals(MovieError.Network, viewModel.uiState.value.error)
        assertEquals(
            listOf(DetailRequest(movieId = 4, language = MovieContentLanguage.EN_US)),
            repository.detailRefreshCalls,
        )

        repository.detailError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            listOf(
                DetailRequest(movieId = 4, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 4, language = MovieContentLanguage.EN_US),
            ),
            repository.detailRefreshCalls,
        )
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun retryIgnoresASecondCallWhileTheRefreshIsRunning() = runViewModelTest {
        repository.detailError = MovieException(MovieError.Network)
        val viewModel = viewModel(movieId = 4)
        advanceUntilIdle()

        repository.detailError = null
        repository.detailGate = CompletableDeferred()
        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repository.detailRefreshCalls.size)

        repository.detailGate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(2, repository.detailRefreshCalls.size)
    }

    @Test
    fun selectMovieKeepsTheCurrentDetailAndCancelsThePreviousRefresh() = runViewModelTest {
        val viewModel = viewModel(movieId = 1)
        advanceUntilIdle()
        viewModel.selectMovie(1)
        advanceUntilIdle()
        assertEquals(
            listOf(DetailRequest(movieId = 1, language = MovieContentLanguage.EN_US)),
            repository.detailRefreshCalls,
        )

        repository.detailGate = CompletableDeferred()
        viewModel.selectMovie(2)
        advanceUntilIdle()
        assertEquals(2, repository.detailRefreshCalls.last().movieId)

        viewModel.selectMovie(3)
        advanceUntilIdle()

        assertEquals(3, repository.detailRefreshCalls.last().movieId)
        assertEquals(3, repository.detailRefreshCalls.size)

        repository.detailGate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(3, repository.detailRefreshCalls.size)
        assertEquals(listOf(1, 2, 3), repository.observedMovieIds)
    }

    @Test
    fun switchingListSelectionsObservesEachMovieAndSkipsAnUnchangedId() = runViewModelTest {
        val viewModel = viewModel(movieId = 1)
        advanceUntilIdle()
        viewModel.clearSelectedMovie()
        advanceUntilIdle()

        viewModel.selectMovie(42)
        advanceUntilIdle()
        viewModel.selectMovie(77)
        advanceUntilIdle()
        viewModel.selectMovie(42)
        advanceUntilIdle()
        val refreshesAfterReturn = repository.detailRefreshCalls.size
        viewModel.selectMovie(42)
        advanceUntilIdle()

        assertEquals(
            listOf(
                DetailRequest(movieId = 1, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 42, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 77, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 42, language = MovieContentLanguage.EN_US),
            ),
            repository.detailRefreshCalls,
        )
        assertEquals(refreshesAfterReturn, repository.detailRefreshCalls.size)
        assertEquals(listOf(1, 42, 77, 42), repository.observedMovieIds)
    }

    @Test
    fun clearSelectedMovieStopsLanguageRefreshUntilAnotherMovieIsSelected() = runViewModelTest {
        val viewModel = viewModel(movieId = 6)
        advanceUntilIdle()
        val refreshesBeforeClear = repository.detailRefreshCalls.size

        viewModel.clearSelectedMovie()
        languageState.updateContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(refreshesBeforeClear, repository.detailRefreshCalls.size)
        assertNull(viewModel.uiState.value.movie)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.selectMovie(9)
        advanceUntilIdle()

        assertEquals(
            DetailRequest(movieId = 9, language = MovieContentLanguage.ES_MX),
            repository.detailRefreshCalls.last(),
        )
    }

    @Test
    fun retryThatFinishesBeforeALanguageChangeIsFollowedByTheNewLanguage() = runViewModelTest {
        repository.detailError = MovieException(MovieError.Network)
        val viewModel = viewModel(movieId = 5)
        advanceUntilIdle()
        repository.detailError = null

        viewModel.retry()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)

        languageState.updateContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(
            listOf(
                DetailRequest(movieId = 5, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 5, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 5, language = MovieContentLanguage.ES_MX),
            ),
            repository.detailRefreshCalls,
        )
        assertTrue(repository.cancelledDetailRequests.isEmpty())
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun languageChangeCancelsARetryInProgress() = runViewModelTest {
        repository.detailError = MovieException(MovieError.Network)
        val viewModel = viewModel(movieId = 5)
        advanceUntilIdle()
        repository.detailError = null
        val englishRetry = CompletableDeferred<Unit>()
        repository.detailResponse = { request ->
            if (request.language == MovieContentLanguage.EN_US) englishRetry.await()
        }

        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoading)

        languageState.updateContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        assertEquals(
            listOf(DetailRequest(movieId = 5, language = MovieContentLanguage.EN_US)),
            repository.cancelledDetailRequests,
        )
        assertEquals(
            DetailRequest(movieId = 5, language = MovieContentLanguage.ES_MX),
            repository.detailRefreshCalls.last(),
        )
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)

        englishRetry.complete(Unit)
        advanceUntilIdle()
        assertEquals(3, repository.detailRefreshCalls.size)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun staleResponseAfterALanguageChangeIsNotPublished() = runViewModelTest {
        repository.detailError = MovieException(MovieError.Network)
        val viewModel = viewModel(movieId = 5)
        advanceUntilIdle()
        repository.detailError = null
        val englishResponse = CompletableDeferred<Unit>()
        val spanishResponse = CompletableDeferred<Unit>()
        repository.detailResponse = { request ->
            if (request.language == MovieContentLanguage.EN_US) {
                withContext(NonCancellable) { englishResponse.await() }
                throw MovieException(MovieError.Server)
            }
            spanishResponse.await()
        }

        viewModel.retry()
        advanceUntilIdle()
        languageState.updateContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()

        englishResponse.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isLoading)

        spanishResponse.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun failedRetryIsFollowedByASuccessfulRetryInTheNewLanguage() = runViewModelTest {
        repository.detailError = MovieException(MovieError.Network)
        val viewModel = viewModel(movieId = 5)
        advanceUntilIdle()

        repository.detailError = MovieException(MovieError.Server)
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(MovieError.Server, viewModel.uiState.value.error)

        repository.detailError = MovieException(MovieError.Network)
        languageState.updateContentLanguage(MovieContentLanguage.ES_MX)
        advanceUntilIdle()
        assertEquals(MovieError.Network, viewModel.uiState.value.error)

        repository.detailError = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            listOf(
                DetailRequest(movieId = 5, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 5, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 5, language = MovieContentLanguage.ES_MX),
                DetailRequest(movieId = 5, language = MovieContentLanguage.ES_MX),
            ),
            repository.detailRefreshCalls,
        )
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun retryIsCancelledWithTheViewModelOrTheSelection() = runViewModelTest {
        repository.detailError = MovieException(MovieError.Network)
        val viewModel = viewModel(movieId = 5)
        advanceUntilIdle()
        repository.detailError = null
        repository.detailGate = CompletableDeferred()

        viewModel.retry()
        advanceUntilIdle()
        viewModel.clearSelectedMovie()
        advanceUntilIdle()
        assertEquals(
            listOf(DetailRequest(movieId = 5, language = MovieContentLanguage.EN_US)),
            repository.cancelledDetailRequests,
        )
        assertNull(viewModel.uiState.value.error)

        viewModel.selectMovie(6)
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()
        viewModel.viewModelScope.cancel()
        advanceUntilIdle()

        assertEquals(
            listOf(
                DetailRequest(movieId = 5, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 6, language = MovieContentLanguage.EN_US),
                DetailRequest(movieId = 6, language = MovieContentLanguage.EN_US),
            ),
            repository.cancelledDetailRequests,
        )
        assertNull(viewModel.uiState.value.error)

        repository.detailGate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(4, repository.detailRefreshCalls.size)
    }

    private fun viewModel(movieId: Int): MovieDetailViewModel {
        return MovieDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("movieId" to movieId)),
            movieRepository = repository,
            contentLanguageState = languageState,
        )
    }
}
