package dev.alexrincon.movieclient.domain.model

data class MovieListSnapshot(
    val movies: List<Movie>,
    val lastLoadedPage: Int,
    val totalPages: Int,
) {
    val canLoadMore: Boolean
        get() = lastLoadedPage > 0 && lastLoadedPage < totalPages

    companion object {
        val Empty = MovieListSnapshot(
            movies = emptyList(),
            lastLoadedPage = 0,
            totalPages = 0,
        )
    }
}
