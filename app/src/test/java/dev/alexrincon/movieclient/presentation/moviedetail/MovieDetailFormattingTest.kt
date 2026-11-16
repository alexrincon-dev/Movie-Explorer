package dev.alexrincon.movieclient.presentation.moviedetail

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieDetailFormattingTest {
    @Test
    fun formatsAnIsoReleaseDate() {
        assertEquals(2010, movieReleaseYear("2010-07-16"))
        assertEquals("Jul 16, 2010", movieReleaseDateLabel("2010-07-16", Locale.US))
    }

    @Test
    fun formatsTheReleaseDateForSpanish() {
        val spanish = movieReleaseDateLabel("2010-07-16", Locale.forLanguageTag("es-MX"))
        assertNotEquals("Jul 16, 2010", spanish)
        assertTrue(spanish!!.contains("2010"))
        assertTrue(spanish.contains("16"))
    }

    @Test
    fun keepsAReadableValueWhenTheDateCannotBeParsed() {
        assertEquals(1999, movieReleaseYear("1999"))
        assertEquals("not-a-date", movieReleaseDateLabel("not-a-date", Locale.US))
    }

    @Test
    fun treatsBlankDatesAsMissing() {
        assertNull(movieReleaseYear(null))
        assertNull(movieReleaseYear("   "))
        assertNull(movieReleaseDateLabel(null, Locale.US))
        assertNull(movieReleaseDateLabel("  ", Locale.US))
    }
}
