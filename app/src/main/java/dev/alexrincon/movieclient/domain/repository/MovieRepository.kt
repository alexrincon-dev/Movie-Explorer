package dev.alexrincon.movieclient.domain.repository

import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListSnapshot
import dev.alexrincon.movieclient.domain.model.MovieListType
import kotlinx.coroutines.flow.Flow

/**
 * Local cache is the source of truth for observation.
 * [refreshMovies], [loadMoreMovies], [refreshAllMovies], and [refreshMovieDetail] throw [MovieException]
 * and must leave cached movies unchanged when they fail.
 *
 * [refreshAllMovies] is the supported way to change the content language of both lists.
 * Callers must not run [refreshMovies] or [loadMoreMovies] in a different language at the same time
 * as each other or during that language change. Popular and Top Rated are not globally locked.
 */
interface MovieRepository {
    fun observeMovies(listType: MovieListType): Flow<MovieListSnapshot>

    fun observeMovie(movieId: Int): Flow<Movie?>

    suspend fun refreshMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    )

    /**
     * Refreshes page 1 of both lists in [language], and only then advances the language generation.
     * The cache is updated only after both requests succeed, because each movie stores one overview.
     */
    suspend fun refreshAllMovies(language: MovieContentLanguage)

    suspend fun loadMoreMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    )

    suspend fun refreshMovieDetail(
        movieId: Int,
        language: MovieContentLanguage,
    )
}
