package dev.alexrincon.movieclient.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

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
                  "title": "Localized",
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

        val movie = page.results.single()

        assertEquals(2, page.page)
        assertEquals(10, page.totalPages)
        assertEquals(200, page.totalResults)
        assertEquals(7, movie.id)
        assertEquals("Localized", movie.title)
        assertEquals("Original", movie.originalTitle)
        assertEquals("", movie.overview)
        assertEquals("/poster.jpg", movie.posterPath)
        assertEquals("", movie.backdropPath)
        assertEquals("", movie.releaseDate)
        assertEquals(8.5, movie.voteAverage ?: 0.0, 0.0)
    }
}
