package dev.alexrincon.movieclient.data.remote.datasource

import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.data.remote.dto.MoviePageDto
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType

interface TmdbRemoteDataSource {
    suspend fun fetchMoviePage(
        listType: MovieListType,
        page: Int,
        language: MovieContentLanguage,
    ): MoviePageDto

    suspend fun fetchMovieDetails(
        movieId: Int,
        language: MovieContentLanguage,
    ): MovieDto
}
