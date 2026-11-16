package dev.alexrincon.movieclient.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.alexrincon.movieclient.domain.model.MovieListType

@Entity(
    tableName = "movie_lists",
    primaryKeys = ["listType", "movieId"],
    foreignKeys = [
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["movieId"]),
        Index(value = ["listType", "sortOrder"]),
    ],
)
data class MovieListEntity(
    val movieId: Int,
    val listType: MovieListType,
    val sortOrder: Int,
    val sourcePage: Int,
)
