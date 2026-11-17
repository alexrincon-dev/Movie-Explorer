package dev.alexrincon.movieclient.data.local.mapper

import dev.alexrincon.movieclient.data.image.TmdbImageUrlBuilder
import dev.alexrincon.movieclient.data.local.entity.MovieEntity
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieEntityMapperTest {
    @Test
    fun toEntity_keepsRelativePathsAndOmitsBlanks() {
        val entity = MovieDto(
            id = 1,
            title = "Localized",
            originalTitle = "Title",
            overview = "Overview",
            posterPath = "/poster.jpg",
            backdropPath = "  ",
            releaseDate = "",
            voteAverage = 7.5,
        ).toEntity(MovieContentLanguage.EN_US)

        assertEquals("Localized", entity.title)
        assertEquals("Title", entity.originalTitle)
        assertEquals("poster.jpg", entity.posterPath)
        assertNull(entity.backdropPath)
        assertNull(entity.releaseDate)
        assertEquals(MovieContentLanguage.EN_US, entity.contentLanguage)
    }

    @Test
    fun toEntity_preservesCachedDetailWhenResponseOmitsIt() {
        val existing = cachedMovie()
        val entity = MovieDto(
            id = 1,
            title = " ",
            originalTitle = "",
            overview = " ",
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            voteAverage = 1.0,
        ).toEntity(MovieContentLanguage.EN_US, existing)

        assertEquals("Cached localized", entity.title)
        assertEquals("Cached title", entity.originalTitle)
        assertEquals("Cached overview", entity.overview)
        assertEquals("poster.jpg", entity.posterPath)
        assertEquals("backdrop.jpg", entity.backdropPath)
        assertEquals("2020-01-01", entity.releaseDate)
        assertEquals(1.0, entity.voteAverage, 0.0)
        assertEquals(MovieContentLanguage.EN_US, entity.contentLanguage)
    }

    @Test
    fun toEntity_languageChangeReplacesOverview() {
        val entity = MovieDto(
            id = 1,
            overview = "",
            posterPath = null,
        ).toEntity(MovieContentLanguage.ES_MX, cachedMovie())

        assertEquals("", entity.title)
        assertEquals("Cached title", entity.originalTitle)
        assertEquals("", entity.overview)
        assertEquals("poster.jpg", entity.posterPath)
        assertEquals(MovieContentLanguage.ES_MX, entity.contentLanguage)
    }

    @Test
    fun toEntity_incomingLocalizedTitleReplacesCachedTitle() {
        val entity = MovieDto(
            id = 1,
            title = "Updated title",
            originalTitle = "",
            overview = "Updated overview",
        ).toEntity(MovieContentLanguage.EN_US, cachedMovie())

        assertEquals("Updated title", entity.title)
        assertEquals("Cached title", entity.originalTitle)
        assertEquals("Updated overview", entity.overview)
    }

    @Test
    fun toEntity_doesNotCopyLocalizedTitleIntoOriginalTitle() {
        val entity = MovieDto(
            id = 1,
            title = "La otra madre",
            originalTitle = " ",
        ).toEntity(MovieContentLanguage.ES_MX, cachedMovie())

        assertEquals("La otra madre", entity.title)
        assertEquals("Cached title", entity.originalTitle)
    }

    @Test
    fun toMovie_restoresBothTitlesAndImageUrls() {
        val movie = cachedMovie().toMovie(TmdbImageUrlBuilder())

        assertEquals("Cached localized", movie.title)
        assertEquals("Cached title", movie.originalTitle)
        assertEquals("Cached overview", movie.overview)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", movie.posterPath)
        assertEquals("https://image.tmdb.org/t/p/w780/backdrop.jpg", movie.backdropPath)
    }

    @Test
    fun toEntity_missingRatingPreservesCachedRating() {
        val entity = MovieDto(id = 1, overview = "Updated").toEntity(
            MovieContentLanguage.EN_US,
            cachedMovie(),
        )

        assertEquals(8.0, entity.voteAverage, 0.0)
        assertEquals("Updated", entity.overview)
    }

    @Test
    fun toEntity_explicitZeroRatingReplacesCachedRating() {
        val entity = MovieDto(id = 1, voteAverage = 0.0).toEntity(
            MovieContentLanguage.EN_US,
            cachedMovie(),
        )

        assertEquals(0.0, entity.voteAverage, 0.0)
    }

    @Test
    fun decode_distinguishesMissingRatingFromExplicitZero() {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val missing = json.decodeFromString<MovieDto>("""{"id":1,"original_title":"Title"}""")
        val explicitZero = json.decodeFromString<MovieDto>("""{"id":1,"vote_average":0.0}""")

        assertEquals("", missing.title)
        assertEquals("Title", missing.originalTitle)
        assertNull(missing.voteAverage)
        assertEquals(0.0, explicitZero.voteAverage)
        assertEquals(
            8.0,
            missing.toEntity(MovieContentLanguage.EN_US, cachedMovie()).voteAverage,
            0.0,
        )
        assertEquals(
            0.0,
            explicitZero.toEntity(MovieContentLanguage.EN_US, cachedMovie()).voteAverage,
            0.0,
        )
    }

    private fun cachedMovie(): MovieEntity {
        return MovieEntity(
            id = 1,
            originalTitle = "Cached title",
            overview = "Cached overview",
            posterPath = "poster.jpg",
            backdropPath = "backdrop.jpg",
            releaseDate = "2020-01-01",
            voteAverage = 8.0,
            contentLanguage = MovieContentLanguage.EN_US,
            title = "Cached localized",
        )
    }
}
