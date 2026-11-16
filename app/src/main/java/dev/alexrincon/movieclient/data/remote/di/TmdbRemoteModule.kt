package dev.alexrincon.movieclient.data.remote.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.alexrincon.movieclient.data.remote.api.TmdbApi
import dev.alexrincon.movieclient.data.remote.datasource.TmdbRemoteDataSource
import dev.alexrincon.movieclient.data.remote.datasource.TmdbRemoteDataSourceImpl
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TmdbRemoteModule {
    @Binds
    @Singleton
    abstract fun bindTmdbRemoteDataSource(
        impl: TmdbRemoteDataSourceImpl,
    ): TmdbRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun provideTmdbApi(retrofit: Retrofit): TmdbApi {
            return retrofit.create(TmdbApi::class.java)
        }
    }
}
