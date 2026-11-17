package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.alexrincon.movieclient.domain.model.MovieListType

/**
 * Selected movie for each list. Owned by the list navigation destination, not by Room.
 * Popular and Top Rated do not share a selection.
 */
class MovieListSelections(
    popularMovieId: Int? = null,
    topRatedMovieId: Int? = null,
) {
    var popularMovieId by mutableStateOf(popularMovieId)
        private set
    var topRatedMovieId by mutableStateOf(topRatedMovieId)
        private set

    fun selectedId(listType: MovieListType): Int? = when (listType) {
        MovieListType.POPULAR -> popularMovieId
        MovieListType.TOP_RATED -> topRatedMovieId
    }

    fun select(listType: MovieListType, movieId: Int?) {
        when (listType) {
            MovieListType.POPULAR -> popularMovieId = movieId
            MovieListType.TOP_RATED -> topRatedMovieId = movieId
        }
    }

    companion object {
        private const val UNSET = -1

        val Saver = listSaver<MovieListSelections, Int>(
            save = { selections ->
                listOf(
                    selections.popularMovieId ?: UNSET,
                    selections.topRatedMovieId ?: UNSET,
                )
            },
            restore = { saved ->
                MovieListSelections(
                    popularMovieId = saved[0].takeUnless { it == UNSET },
                    topRatedMovieId = saved[1].takeUnless { it == UNSET },
                )
            },
        )
    }
}

@Composable
fun rememberMovieListSelections(): MovieListSelections {
    return rememberSaveable(saver = MovieListSelections.Saver) { MovieListSelections() }
}
