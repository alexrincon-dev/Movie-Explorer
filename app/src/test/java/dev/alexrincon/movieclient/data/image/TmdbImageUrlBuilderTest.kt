package dev.alexrincon.movieclient.data.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbImageUrlBuilderTest {
    private val imageUrls = TmdbImageUrlBuilder()

    @Test
    fun posterUrl_usesW500AndStripsLeadingSlashes() {
        assertEquals(
            "https://image.tmdb.org/t/p/w500/abc.jpg",
            imageUrls.posterUrl("/abc.jpg"),
        )
        assertEquals(
            "https://image.tmdb.org/t/p/w500/abc.jpg",
            imageUrls.posterUrl("abc.jpg"),
        )
    }

    @Test
    fun backdropUrl_usesW780() {
        assertEquals(
            "https://image.tmdb.org/t/p/w780/backdrop.jpg",
            imageUrls.backdropUrl("/backdrop.jpg"),
        )
    }

    @Test
    fun imageUrl_returnsNullForNullOrBlankPaths() {
        assertNull(imageUrls.posterUrl(null))
        assertNull(imageUrls.posterUrl(""))
        assertNull(imageUrls.posterUrl("   "))
        assertNull(imageUrls.backdropUrl("/"))
    }
}
