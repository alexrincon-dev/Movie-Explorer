package dev.alexrincon.movieclient.presentation.movielist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListSnapshot
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.domain.repository.MovieRepository
import dev.alexrincon.movieclient.domain.usecase.LoadMoreMoviesUseCase
import dev.alexrincon.movieclient.domain.usecase.RefreshMoviesUseCase
import dev.alexrincon.movieclient.presentation.language.MovieContentLanguageState
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val refreshMovies: RefreshMoviesUseCase,
    private val loadMoreMovies: LoadMoreMoviesUseCase,
    private val contentLanguageState: MovieContentLanguageState,
) : ViewModel() {

    private val selectedListType = MutableStateFlow(MovieListType.POPULAR)
    private val status = MutableStateFlow(ListStatus())
    private val listsRefreshed = mutableSetOf<MovieListType>()
    private val refreshTokens = mutableMapOf<MovieListType, Int>()
    private val refreshJobs = mutableMapOf<MovieListType, Job>()
    private var loadMoreJob: Job? = null
    private var loadMoreInFlight = false
    private var languageChangeInFlight = false

    val uiState: StateFlow<MovieListUiState> = combine(
        selectedListType.flatMapLatest { listType ->
            movieRepository.observeMovies(listType).map { snapshot -> listType to snapshot }
        },
        contentLanguageState.contentLanguage,
        status,
    ) { observed, language, listStatus ->
        val (listType, snapshot) = observed
        listStatus.toUiState(listType, snapshot, language)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MovieListUiState(),
    )

    init {
        viewModelScope.launch {
            contentLanguageState.awaitLanguage()
            ensureInitialRefresh(selectedListType.value)
        }
    }

    fun selectListType(listType: MovieListType) {
        if (selectedListType.value == listType) return
        loadMoreJob?.cancel()
        selectedListType.value = listType
        when {
            languageChangeInFlight -> {
                status.value = status.value.copy(isLoadingMore = false, isRefreshing = true)
            }
            listType in listsRefreshed -> {
                status.value = idleStatus()
            }
            refreshJobs[listType]?.isActive == true -> {
                status.value = status.value.copy(
                    isInitialLoading = true,
                    isRefreshing = true,
                    isLoadingMore = false,
                    error = null,
                    isPaginationError = false,
                )
            }
            else -> {
                status.value = status.value.copy(
                    isInitialLoading = true,
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = null,
                    isPaginationError = false,
                )
                ensureInitialRefresh(listType)
            }
        }
    }

    fun refresh() {
        if (languageChangeInFlight) return
        startRefresh(selectedListType.value, initial = false)
    }

    fun loadMore() {
        val language = contentLanguageState.contentLanguage.value ?: return
        val current = uiState.value
        if (loadMoreInFlight || languageChangeInFlight || !current.canLoadMore) return
        if (current.selectedListType !in listsRefreshed) return
        if (refreshJobs[current.selectedListType]?.isActive == true) return
        loadMoreInFlight = true
        val listType = current.selectedListType
        loadMoreJob = viewModelScope.launch {
            if (selectedListType.value == listType) {
                status.value = status.value.copy(
                    isLoadingMore = true,
                    error = null,
                    isPaginationError = false,
                )
            }
            try {
                loadMoreMovies(listType, language)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: MovieException) {
                if (selectedListType.value == listType && !languageChangeInFlight) {
                    status.value = status.value.copy(
                        error = exception.error,
                        isPaginationError = true,
                    )
                }
            } finally {
                loadMoreInFlight = false
                if (selectedListType.value == listType && !languageChangeInFlight) {
                    status.value = status.value.copy(isLoadingMore = false)
                }
            }
        }
    }

    /**
     * Stores the selected language before refreshing either list.
     * The preference and UI language stay on that selection when the refresh fails.
     * A failed refresh leaves the cached movies in place.
     * When the language cannot be saved, the language does not change, no refresh starts,
     * and the list shows [MovieError.Unknown].
     */
    fun changeContentLanguage(language: MovieContentLanguage) {
        val current = contentLanguageState.contentLanguage.value ?: return
        if (language == current || languageChangeInFlight) {
            return
        }
        languageChangeInFlight = true
        status.value = status.value.copy(
            isRefreshing = true,
            isLoadingMore = false,
            error = null,
            isPaginationError = false,
        )
        viewModelScope.launch {
            try {
                contentLanguageState.updateContentLanguage(language)
                refreshJobs.values.toList().forEach { it.cancel() }
                loadMoreJob?.cancel()
                listsRefreshed.clear()
                movieRepository.refreshAllMovies(language)
                listsRefreshed.addAll(MovieListType.entries)
                status.value = idleStatus()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: MovieException) {
                status.value = status.value.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    error = exception.error,
                    isPaginationError = false,
                )
            } catch (_: IOException) {
                status.value = status.value.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    error = MovieError.Unknown,
                    isPaginationError = false,
                )
            } finally {
                languageChangeInFlight = false
            }
        }
    }

    private fun ensureInitialRefresh(listType: MovieListType) {
        if (languageChangeInFlight || listType in listsRefreshed) return
        if (refreshJobs[listType]?.isActive == true) return
        startRefresh(listType, initial = true)
    }

    private fun startRefresh(listType: MovieListType, initial: Boolean) {
        if (languageChangeInFlight) return
        if (initial && listType in listsRefreshed) return
        if (initial && refreshJobs[listType]?.isActive == true) return
        val language = contentLanguageState.contentLanguage.value ?: return
        loadMoreJob?.cancel()
        val token = (refreshTokens[listType] ?: 0) + 1
        refreshTokens[listType] = token
        refreshJobs[listType]?.cancel()
        refreshJobs[listType] = viewModelScope.launch {
            if (selectedListType.value == listType) {
                status.value = status.value.copy(
                    isInitialLoading = initial,
                    isRefreshing = true,
                    isLoadingMore = false,
                    error = null,
                    isPaginationError = false,
                )
            }
            try {
                refreshMovies(listType, language)
                listsRefreshed += listType
                if (isCurrentRefresh(listType, token)) {
                    status.value = idleStatus()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: MovieException) {
                if (isCurrentRefresh(listType, token)) {
                    status.value = status.value.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        error = exception.error,
                        isPaginationError = false,
                    )
                }
            }
        }
    }

    private fun isCurrentRefresh(listType: MovieListType, token: Int): Boolean {
        return token == refreshTokens[listType] &&
            selectedListType.value == listType &&
            !languageChangeInFlight
    }

    private fun idleStatus(): ListStatus = ListStatus(isInitialLoading = false)

    private data class ListStatus(
        val isInitialLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: MovieError? = null,
        val isPaginationError: Boolean = false,
    )

    private fun ListStatus.toUiState(
        listType: MovieListType,
        snapshot: MovieListSnapshot,
        language: MovieContentLanguage?,
    ): MovieListUiState {
        val showInitialLoader = isInitialLoading && snapshot.movies.isEmpty()
        return MovieListUiState(
            movies = snapshot.movies,
            selectedListType = listType,
            contentLanguage = language,
            isInitialLoading = showInitialLoader,
            isRefreshing = isRefreshing && !showInitialLoader,
            isLoadingMore = isLoadingMore,
            canLoadMore = snapshot.canLoadMore,
            error = error,
            isPaginationError = isPaginationError && error != null,
        )
    }
}
