package dev.alexrincon.movieclient.presentation

import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListSnapshot
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.domain.repository.MovieRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

internal class FakeMovieRepository : MovieRepository {
    val listSnapshots: Map<MovieListType, MutableStateFlow<MovieListSnapshot>> =
        MovieListType.entries.associateWith { MutableStateFlow(MovieListSnapshot.Empty) }
    val movieSnapshots = mutableMapOf<Int, MutableStateFlow<Movie?>>()

    val observedMovieIds = mutableListOf<Int>()
    val refreshCalls = mutableListOf<ListRequest>()
    val loadMoreCalls = mutableListOf<ListRequest>()
    val refreshAllCalls = mutableListOf<MovieContentLanguage>()
    val detailRefreshCalls = mutableListOf<DetailRequest>()
    var activePopularCollections = 0

    var refreshGate: CompletableDeferred<Unit>? = null
    var loadMoreGate: CompletableDeferred<Unit>? = null
    var refreshAllGate: CompletableDeferred<Unit>? = null
    var detailGate: CompletableDeferred<Unit>? = null
    var refreshError: Throwable? = null
    var loadMoreError: Throwable? = null
    var refreshAllError: Throwable? = null
    var detailError: Throwable? = null

    override fun observeMovies(listType: MovieListType): Flow<MovieListSnapshot> {
        val upstream = listSnapshots.getValue(listType)
        if (listType != MovieListType.POPULAR) return upstream
        return upstream
            .onStart { activePopularCollections += 1 }
            .onCompletion { activePopularCollections -= 1 }
    }

    override fun observeMovie(movieId: Int): Flow<Movie?> {
        observedMovieIds += movieId
        return movieSnapshots.getOrPut(movieId) { MutableStateFlow(null) }
    }

    override suspend fun refreshMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    ) {
        refreshCalls += ListRequest(listType, language)
        refreshGate?.await()
        refreshError?.let { throw it }
    }

    override suspend fun refreshAllMovies(language: MovieContentLanguage) {
        refreshAllCalls += language
        refreshAllGate?.await()
        refreshAllError?.let { throw it }
    }

    override suspend fun loadMoreMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    ) {
        loadMoreCalls += ListRequest(listType, language)
        loadMoreGate?.await()
        loadMoreError?.let { throw it }
    }

    override suspend fun refreshMovieDetail(
        movieId: Int,
        language: MovieContentLanguage,
    ) {
        detailRefreshCalls += DetailRequest(movieId, language)
        detailGate?.await()
        detailError?.let { throw it }
    }
}

internal data class ListRequest(
    val listType: MovieListType,
    val language: MovieContentLanguage,
)

internal data class DetailRequest(
    val movieId: Int,
    val language: MovieContentLanguage,
)

internal fun testMovie(id: Int): Movie {
    return Movie(
        id = id,
        title = "Movie $id",
        originalTitle = "Movie $id",
        overview = "Overview $id",
        posterPath = null,
        backdropPath = null,
        releaseDate = null,
        voteAverage = 7.0,
    )
}

internal fun testSnapshot(
    movies: List<Movie>,
    lastLoadedPage: Int = 1,
    totalPages: Int = 1,
): MovieListSnapshot {
    return MovieListSnapshot(
        movies = movies,
        lastLoadedPage = lastLoadedPage,
        totalPages = totalPages,
    )
}
