package dev.alexrincon.movieclient.presentation

import dev.alexrincon.movieclient.domain.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieTitleTextTest {
    @Test
    fun displayTitleUsesTheLocalizedTitle() {
        assertEquals("La otra madre", movie(title = "La otra madre", originalTitle = "Other Mommy").displayTitle())
    }

    @Test
    fun displayTitleFallsBackWhenTheLocalizedTitleIsBlank() {
        assertEquals("Other Mommy", movie(title = "  ", originalTitle = " Other Mommy ").displayTitle())
    }

    @Test
    fun distinctOriginalTitleIsShownOnlyWhenTheTitlesDiffer() {
        assertEquals(
            "Other Mommy",
            movie(title = "La otra madre", originalTitle = "Other Mommy").distinctOriginalTitle(),
        )
        assertNull(movie(title = "Inception", originalTitle = "Inception").distinctOriginalTitle())
        assertNull(movie(title = "Inception", originalTitle = " Inception ").distinctOriginalTitle())
        assertNull(movie(title = "", originalTitle = "Other Mommy").distinctOriginalTitle())
        assertNull(movie(title = "La otra madre", originalTitle = " ").distinctOriginalTitle())
    }

    private fun movie(title: String, originalTitle: String): Movie {
        return Movie(
            id = 1,
            title = title,
            originalTitle = originalTitle,
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            voteAverage = 0.0,
        )
    }
}
