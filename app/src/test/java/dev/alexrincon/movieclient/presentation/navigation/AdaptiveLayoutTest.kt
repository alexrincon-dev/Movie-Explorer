package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun narrowWidthUsesPhoneNavigation() {
        assertFalse(usesMasterDetailLayout(599.dp))
        assertFalse(usesMasterDetailLayout(839.dp))
    }

    @Test
    fun expandedWidthUsesMasterDetail() {
        assertTrue(usesMasterDetailLayout(840.dp))
        assertTrue(usesMasterDetailLayout(1200.dp))
    }

    @Test
    fun initialSelectionUsesTheFirstMovie() {
        assertEquals(4, resolveSelectedMovieId(selectedMovieId = null, movieIds = listOf(4, 8, 15)))
    }

    @Test
    fun tappingAnotherMovieKeepsThatSelection() {
        assertEquals(8, resolveSelectedMovieId(selectedMovieId = 8, movieIds = listOf(4, 8, 15)))
    }

    @Test
    fun switchingListsKeepsTheMovieWhenItIsStillPresent() {
        assertEquals(8, resolveSelectedMovieId(selectedMovieId = 8, movieIds = listOf(8, 20)))
    }

    @Test
    fun switchingListsSelectsTheFirstMovieWhenTheSelectionIsGone() {
        assertEquals(20, resolveSelectedMovieId(selectedMovieId = 8, movieIds = listOf(20, 21)))
    }

    @Test
    fun settledEmptyListClearsTheSelection() {
        assertEquals(null, resolveSelectedMovieId(selectedMovieId = 8, movieIds = emptyList()))
        assertEquals(null, resolveSelectedMovieId(selectedMovieId = null, movieIds = emptyList()))
    }

    @Test
    fun loadingEmptyListKeepsTheSelection() {
        assertEquals(
            8,
            resolveSelectedMovieId(
                selectedMovieId = 8,
                movieIds = emptyList(),
                keepSelectionWhenEmpty = true,
            ),
        )
    }

    @Test
    fun sameMoviesDoNotChangeTheSelection() {
        val movies = listOf(4, 8, 15)
        val selected = resolveSelectedMovieId(selectedMovieId = 8, movieIds = movies)
        assertEquals(selected, resolveSelectedMovieId(selectedMovieId = selected, movieIds = movies))
    }
}
