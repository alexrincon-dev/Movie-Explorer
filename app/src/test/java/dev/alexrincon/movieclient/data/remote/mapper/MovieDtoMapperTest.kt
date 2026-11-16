package dev.alexrincon.movieclient.data.remote.mapper

import dev.alexrincon.movieclient.data.image.TmdbImageUrlBuilder
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.data.remote.dto.MoviePageDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDtoMapperTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val imageUrls = TmdbImageUrlBuilder()

    @Test
    fun decode_mapsTmdbFieldsAndIgnoresUnknownProperties() {
        val page = json.decodeFromString<MoviePageDto>(
            """
            {
              "page": 2,
              "total_pages": 10,
              "total_results": 200,
              "results": [
                {
                  "id": 7,
                  "original_title": "Original",
                  "overview": null,
                  "poster_path": "/poster.jpg",
                  "backdrop_path": "",
                  "release_date": "",
                  "vote_average": 8.5,
                  "adult": false
                }
              ]
            }
            """.trimIndent(),
        )

        val movie = page.results.single().toMovie(imageUrls)

        assertEquals(2, page.page)
        assertEquals(10, page.totalPages)
        assertEquals(200, page.totalResults)
        assertEquals(7, movie.id)
        assertEquals("Original", movie.originalTitle)
        assertEquals("", movie.overview)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", movie.posterPath)
        assertNull(movie.backdropPath)
        assertNull(movie.releaseDate)
        assertEquals(8.5, movie.voteAverage, 0.0)
    }
}
