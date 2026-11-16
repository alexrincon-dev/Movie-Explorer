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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movieRepository: MovieRepository,
    private val contentLanguageState: MovieContentLanguageState,
) : ViewModel() {

    private val movieId: Int = savedStateHandle.toRoute<MovieDetailRoute>().movieId
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<MovieError?>(null)
    private var retryJob: Job? = null

    val uiState: StateFlow<MovieDetailUiState> = combine(
        movieRepository.observeMovie(movieId),
        loading,
        error,
    ) { movie, isLoading, movieError ->
        MovieDetailUiState(
            movie = movie,
            isLoading = isLoading && movie == null,
            error = movieError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MovieDetailUiState(isLoading = true),
    )

    init {
        viewModelScope.launch {
            contentLanguageState.contentLanguage.collectLatest { language ->
                refreshDetail(language)
            }
        }
    }

    fun retry() {
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            refreshDetail(contentLanguageState.contentLanguage.value)
        }
    }

    private suspend fun refreshDetail(language: MovieContentLanguage) {
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
