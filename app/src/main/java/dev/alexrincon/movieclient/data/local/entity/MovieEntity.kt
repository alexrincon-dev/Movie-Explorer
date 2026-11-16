package dev.alexrincon.movieclient.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Int,
    val originalTitle: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val contentLanguage: MovieContentLanguage,
)
