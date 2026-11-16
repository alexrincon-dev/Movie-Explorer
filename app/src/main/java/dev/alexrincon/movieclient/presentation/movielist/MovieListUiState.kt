package dev.alexrincon.movieclient.presentation.movielist

import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType

data class MovieListUiState(
    val movies: List<Movie> = emptyList(),
    val selectedListType: MovieListType = MovieListType.POPULAR,
    val contentLanguage: MovieContentLanguage = MovieContentLanguage.EN_US,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: MovieError? = null,
    val isPaginationError: Boolean = false,
) {
    val retriesFailedPage: Boolean
        get() = movies.isNotEmpty() && isPaginationError
}
