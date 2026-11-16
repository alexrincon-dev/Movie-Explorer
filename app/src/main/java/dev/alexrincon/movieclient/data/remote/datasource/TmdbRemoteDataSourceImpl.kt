package dev.alexrincon.movieclient.data.remote.datasource

import dev.alexrincon.movieclient.core.network.TmdbApiKey
import dev.alexrincon.movieclient.data.remote.api.TmdbApi
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.data.remote.dto.MoviePageDto
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRemoteDataSourceImpl @Inject constructor(
    private val api: TmdbApi,
    @param:TmdbApiKey private val apiKey: String,
) : TmdbRemoteDataSource {

    override suspend fun fetchMoviePage(
        listType: MovieListType,
        page: Int,
        language: MovieContentLanguage,
    ): MoviePageDto {
        val languageTag = language.toTmdbLanguageTag()
        return when (listType) {
            MovieListType.POPULAR -> api.getPopularMovies(
                page = page,
                language = languageTag,
                apiKey = apiKey,
            )
            MovieListType.TOP_RATED -> api.getTopRatedMovies(
                page = page,
                language = languageTag,
                apiKey = apiKey,
            )
        }
    }

    override suspend fun fetchMovieDetails(
        movieId: Int,
        language: MovieContentLanguage,
    ): MovieDto {
        return api.getMovieDetails(
            movieId = movieId,
            language = language.toTmdbLanguageTag(),
            apiKey = apiKey,
        )
    }
}

internal fun MovieContentLanguage.toTmdbLanguageTag(): String {
    return when (this) {
        MovieContentLanguage.EN_US -> "en-US"
        MovieContentLanguage.ES_MX -> "es-MX"
    }
}
