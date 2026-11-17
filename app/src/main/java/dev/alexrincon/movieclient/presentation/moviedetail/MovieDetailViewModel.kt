package dev.alexrincon.movieclient.presentation.moviedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.repository.MovieRepository
import dev.alexrincon.movieclient.presentation.language.MovieContentLanguageState
import dev.alexrincon.movieclient.presentation.navigation.MovieDetailRoute
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movieRepository: MovieRepository,
    private val contentLanguageState: MovieContentLanguageState,
) : ViewModel() {

    private val movieIdState = MutableStateFlow(initialMovieId(savedStateHandle))
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<MovieError?>(null)
    private var retryJob: Job? = null

    val uiState: StateFlow<MovieDetailUiState> = movieIdState.flatMapLatest { movieId ->
        if (movieId == null) {
            flowOf(MovieDetailUiState(isLoading = true))
        } else {
            combine(
                movieRepository.observeMovie(movieId),
                loading,
                error,
            ) { movie, isLoading, movieError ->
                MovieDetailUiState(
                    movie = movie,
                    isLoading = isLoading && movie == null,
                    error = movieError,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MovieDetailUiState(isLoading = true),
    )

    init {
        viewModelScope.launch {
            movieIdState.collectLatest { movieId ->
                if (movieId == null) return@collectLatest
                contentLanguageState.contentLanguage.collectLatest { language ->
                    refreshDetail(movieId, language)
                }
            }
        }
    }

    fun selectMovie(movieId: Int) {
        if (movieIdState.value == movieId) return
        retryJob?.cancel()
        loading.value = true
        error.value = null
        movieIdState.value = movieId
    }

    /**
     * Stops detail observation when the tablet pane leaves composition.
     * The list destination may keep this ViewModel, so clearing the id cancels its work.
     */
    fun clearSelectedMovie() {
        retryJob?.cancel()
        movieIdState.value = null
    }

    fun retry() {
        val movieId = movieIdState.value ?: return
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            refreshDetail(movieId, contentLanguageState.contentLanguage.value)
        }
    }

    private suspend fun refreshDetail(movieId: Int, language: MovieContentLanguage) {
        loading.value = true
        error.value = null
        try {
            movieRepository.refreshMovieDetail(movieId, language)
            loading.value = false
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: MovieException) {
            loading.value = false
            error.value = exception.error
        }
    }
}

private fun initialMovieId(savedStateHandle: SavedStateHandle): Int? {
    savedStateHandle.get<Int>("movieId")?.let { return it }
    return runCatching { savedStateHandle.toRoute<MovieDetailRoute>().movieId }.getOrNull()
}
