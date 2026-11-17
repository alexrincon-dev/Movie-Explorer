package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expanded window width from the Android window size classes.
 * At this width the list can keep two poster columns and the detail pane still has room.
 */
val MasterDetailMinWidth: Dp = 840.dp

fun usesMasterDetailLayout(maxWidth: Dp): Boolean = maxWidth >= MasterDetailMinWidth

/**
 * Keeps [selectedMovieId] when that movie is still in [movieIds].
 * An empty list does not clear the selection, so a refresh cannot reset it.
 */
fun resolveSelectedMovieId(selectedMovieId: Int?, movieIds: List<Int>): Int? {
    if (movieIds.isEmpty()) return selectedMovieId
    if (selectedMovieId != null && selectedMovieId in movieIds) return selectedMovieId
    return movieIds.first()
}
