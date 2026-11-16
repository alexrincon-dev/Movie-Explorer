package dev.alexrincon.movieclient.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.alexrincon.movieclient.data.local.entity.MovieListPaginationEntity
import dev.alexrincon.movieclient.domain.model.MovieListType
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieListPaginationDao {
    @Query("SELECT * FROM movie_list_pagination WHERE listType = :listType")
    fun observe(listType: MovieListType): Flow<MovieListPaginationEntity?>

    @Query("SELECT * FROM movie_list_pagination WHERE listType = :listType")
    suspend fun get(listType: MovieListType): MovieListPaginationEntity?

    @Upsert
    suspend fun upsert(pagination: MovieListPaginationEntity)
}
