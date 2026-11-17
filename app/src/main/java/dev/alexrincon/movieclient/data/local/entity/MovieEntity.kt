package dev.alexrincon.movieclient.data.local.entity

import androidx.room.ColumnInfo
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
    /**
     * Localized title. Kept last so a version 1 database can add it with ALTER TABLE
     * and still match this schema.
     */
    @ColumnInfo(defaultValue = "''")
    val title: String,
)
