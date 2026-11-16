package dev.alexrincon.movieclient.domain.repository

import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListSnapshot
import dev.alexrincon.movieclient.domain.model.MovieListType
import kotlinx.coroutines.flow.Flow

/**
 * Local cache is the source of truth for observation.
 * [refreshMovies], [loadMoreMovies], and [refreshMovieDetail] throw [MovieException]
 * and must leave cached movies unchanged when they fail.
 */
interface MovieRepository {
    fun observeMovies(listType: MovieListType): Flow<MovieListSnapshot>

    fun observeMovie(movieId: Int): Flow<Movie?>

    suspend fun refreshMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    )

    suspend fun loadMoreMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    )

    suspend fun refreshMovieDetail(
        movieId: Int,
        language: MovieContentLanguage,
    )
}
