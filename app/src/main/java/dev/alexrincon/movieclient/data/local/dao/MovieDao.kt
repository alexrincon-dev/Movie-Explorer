package dev.alexrincon.movieclient.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.alexrincon.movieclient.data.local.entity.MovieEntity
import dev.alexrincon.movieclient.domain.model.MovieListType
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query(
        """
        SELECT
            movies.id,
            movies.originalTitle,
            movies.overview,
            movies.posterPath,
            movies.backdropPath,
            movies.releaseDate,
            movies.voteAverage,
            movies.contentLanguage,
            movies.title
        FROM movies
        INNER JOIN movie_lists ON movies.id = movie_lists.movieId
        WHERE movie_lists.listType = :listType
        ORDER BY movie_lists.sortOrder ASC, movie_lists.movieId ASC
        """,
    )
    fun observeByListType(listType: MovieListType): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    fun observeById(movieId: Int): Flow<MovieEntity?>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<MovieEntity>

    @Upsert
    suspend fun upsertAll(movies: List<MovieEntity>)
}
