package dev.alexrincon.movieclient.data.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.alexrincon.movieclient.data.local.dao.MovieDao
import dev.alexrincon.movieclient.data.local.dao.MovieListDao
import dev.alexrincon.movieclient.data.local.dao.MovieListPaginationDao
import dev.alexrincon.movieclient.data.local.database.MovieDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideMovieDatabase(
        @ApplicationContext context: Context,
    ): MovieDatabase {
        return Room.databaseBuilder(
            context,
            MovieDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }

    @Provides
    fun provideMovieDao(database: MovieDatabase): MovieDao = database.movieDao()

    @Provides
    fun provideMovieListDao(database: MovieDatabase): MovieListDao = database.movieListDao()

    @Provides
    fun provideMovieListPaginationDao(
        database: MovieDatabase,
    ): MovieListPaginationDao = database.movieListPaginationDao()

    private const val DATABASE_NAME = "movies.db"
}
