package dev.alexrincon.movieclient.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.alexrincon.movieclient.data.local.dao.MovieDao
import dev.alexrincon.movieclient.data.local.dao.MovieListDao
import dev.alexrincon.movieclient.data.local.dao.MovieListPaginationDao
import dev.alexrincon.movieclient.data.local.entity.MovieEntity
import dev.alexrincon.movieclient.data.local.entity.MovieListEntity
import dev.alexrincon.movieclient.data.local.entity.MovieListPaginationEntity

@Database(
    entities = [
        MovieEntity::class,
        MovieListEntity::class,
        MovieListPaginationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(MovieTypeConverters::class)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    abstract fun movieListDao(): MovieListDao

    abstract fun movieListPaginationDao(): MovieListPaginationDao
}
