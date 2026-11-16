package dev.alexrincon.movieclient.data.local.database

import androidx.room.TypeConverter
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType

class MovieTypeConverters {
    @TypeConverter
    fun toMovieListType(value: String): MovieListType = MovieListType.valueOf(value)

    @TypeConverter
    fun fromMovieListType(value: MovieListType): String = value.name

    @TypeConverter
    fun toMovieContentLanguage(value: String): MovieContentLanguage = MovieContentLanguage.valueOf(value)

    @TypeConverter
    fun fromMovieContentLanguage(value: MovieContentLanguage): String = value.name
}
