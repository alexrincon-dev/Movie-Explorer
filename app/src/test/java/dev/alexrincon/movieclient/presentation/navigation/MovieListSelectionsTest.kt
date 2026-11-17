package dev.alexrincon.movieclient.presentation.navigation

import dev.alexrincon.movieclient.domain.model.MovieListType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieListSelectionsTest {
    @Test
    fun eachListRestoresItsOwnSelectedMovie() {
        val selections = MovieListSelections()
        val popular = listOf(1, 8, 9)
        val topRated = listOf(20, 21, 22)

        selections.select(MovieListType.POPULAR, displayedId(selections, MovieListType.POPULAR, popular))
        selections.select(MovieListType.POPULAR, 8)
        selections.select(MovieListType.TOP_RATED, displayedId(selections, MovieListType.TOP_RATED, topRated))
        selections.select(MovieListType.TOP_RATED, 21)

        assertEquals(8, displayedId(selections, MovieListType.POPULAR, popular))
        assertEquals(21, displayedId(selections, MovieListType.TOP_RATED, topRated))
        assertEquals(8, selections.selectedId(MovieListType.POPULAR))
        assertEquals(21, selections.selectedId(MovieListType.TOP_RATED))
    }

    @Test
    fun tabWithoutAPreviousSelectionUsesItsFirstMovie() {
        val selections = MovieListSelections(popularMovieId = 8)

        assertEquals(20, displayedId(selections, MovieListType.TOP_RATED, listOf(20, 21)))
        assertEquals(8, selections.selectedId(MovieListType.POPULAR))
    }

    @Test
    fun missingMovieFallsBackToTheFirstMovieInThatList() {
        val selections = MovieListSelections(popularMovieId = 8, topRatedMovieId = 21)

        assertEquals(4, displayedId(selections, MovieListType.POPULAR, listOf(4, 5)))
        assertEquals(21, selections.selectedId(MovieListType.TOP_RATED))
    }

    @Test
    fun emptyListClearsOnlyThatSelection() {
        val selections = MovieListSelections(popularMovieId = 8, topRatedMovieId = 21)

        assertNull(displayedId(selections, MovieListType.POPULAR, emptyList()))
        selections.select(MovieListType.POPULAR, null)

        assertNull(selections.selectedId(MovieListType.POPULAR))
        assertEquals(21, selections.selectedId(MovieListType.TOP_RATED))
        assertEquals(21, displayedId(selections, MovieListType.TOP_RATED, listOf(20, 21)))
    }

    @Test
    fun snapshotThatStillContainsTheMovieDoesNotResetIt() {
        val selections = MovieListSelections(popularMovieId = 8)
        val firstSnapshot = listOf(4, 8, 15)
        val nextSnapshot = listOf(4, 8, 15, 16)

        assertEquals(8, displayedId(selections, MovieListType.POPULAR, firstSnapshot))
        assertEquals(8, displayedId(selections, MovieListType.POPULAR, nextSnapshot))
        assertEquals(8, selections.selectedId(MovieListType.POPULAR))
    }

    @Test
    fun loadingEmptyListDoesNotClearARestoredSelection() {
        val selections = MovieListSelections(popularMovieId = 8, topRatedMovieId = 21)

        assertEquals(
            8,
            displayedId(
                selections,
                MovieListType.POPULAR,
                movieIds = emptyList(),
                keepSelectionWhenEmpty = true,
            ),
        )
        assertEquals(21, selections.selectedId(MovieListType.TOP_RATED))
    }

    private fun displayedId(
        selections: MovieListSelections,
        listType: MovieListType,
        movieIds: List<Int>,
        keepSelectionWhenEmpty: Boolean = false,
    ): Int? {
        return resolveSelectedMovieId(
            selectedMovieId = selections.selectedId(listType),
            movieIds = movieIds,
            keepSelectionWhenEmpty = keepSelectionWhenEmpty,
        )
    }
}
