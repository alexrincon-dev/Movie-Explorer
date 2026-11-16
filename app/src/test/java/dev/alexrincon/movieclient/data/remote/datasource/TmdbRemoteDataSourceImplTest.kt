package dev.alexrincon.movieclient.data.remote.datasource

import dev.alexrincon.movieclient.data.remote.api.TmdbApi
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.data.remote.dto.MoviePageDto
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbRemoteDataSourceImplTest {
    @Test
    fun fetchMoviePage_selectsEndpointAndMapsLanguage() = runBlocking {
        val api = RecordingTmdbApi()
        val dataSource = TmdbRemoteDataSourceImpl(api, API_KEY)

        dataSource.fetchMoviePage(
            listType = MovieListType.POPULAR,
            page = 2,
            language = MovieContentLanguage.EN_US,
        )
        dataSource.fetchMoviePage(
            listType = MovieListType.TOP_RATED,
            page = 3,
            language = MovieContentLanguage.ES_MX,
        )

        assertEquals(1, api.popularCalls.size)
        assertEquals(Call(page = 2, language = "en-US", apiKey = API_KEY), api.popularCalls.single())
        assertEquals(1, api.topRatedCalls.size)
        assertEquals(Call(page = 3, language = "es-MX", apiKey = API_KEY), api.topRatedCalls.single())
    }

    @Test
    fun fetchMovieDetails_passesIdLanguageAndApiKey() = runBlocking {
        val api = RecordingTmdbApi()
        val dataSource = TmdbRemoteDataSourceImpl(api, API_KEY)

        dataSource.fetchMovieDetails(
            movieId = 42,
            language = MovieContentLanguage.ES_MX,
        )

        assertEquals(
            DetailCall(movieId = 42, language = "es-MX", apiKey = API_KEY),
            api.detailCalls.single(),
        )
    }

    private class RecordingTmdbApi : TmdbApi {
        val popularCalls = mutableListOf<Call>()
        val topRatedCalls = mutableListOf<Call>()
        val detailCalls = mutableListOf<DetailCall>()

        override suspend fun getPopularMovies(
            page: Int,
            language: String,
            apiKey: String,
        ): MoviePageDto {
            popularCalls += Call(page, language, apiKey)
            return MoviePageDto()
        }

        override suspend fun getTopRatedMovies(
            page: Int,
            language: String,
            apiKey: String,
        ): MoviePageDto {
            topRatedCalls += Call(page, language, apiKey)
            return MoviePageDto()
        }

        override suspend fun getMovieDetails(
            movieId: Int,
            language: String,
            apiKey: String,
        ): MovieDto {
            detailCalls += DetailCall(movieId, language, apiKey)
            return MovieDto(id = movieId)
        }
    }

    private data class Call(
        val page: Int,
        val language: String,
        val apiKey: String,
    )

    private data class DetailCall(
        val movieId: Int,
        val language: String,
        val apiKey: String,
    )

    private companion object {
        const val API_KEY = "test-key"
    }
}
