package dev.alexrincon.movieclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.alexrincon.movieclient.domain.model.MovieListType

@Entity(tableName = "movie_list_pagination")
data class MovieListPaginationEntity(
    @PrimaryKey val listType: MovieListType,
    val lastLoadedPage: Int,
    val totalPages: Int,
    val totalResults: Int,
    val updatedAt: Long,
)
