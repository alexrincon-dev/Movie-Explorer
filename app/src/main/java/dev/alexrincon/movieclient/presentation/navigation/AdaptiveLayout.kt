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
 * Movie shown for one list.
 * A list that still contains [selectedMovieId] keeps it, including after a new snapshot.
 * A settled empty list clears the selection. An empty list that is still loading keeps it,
 * so a restored selection survives the first frame and a refresh cannot wipe it.
 * A missing id falls back to the first movie in this list.
 */
fun resolveSelectedMovieId(
    selectedMovieId: Int?,
    movieIds: List<Int>,
    keepSelectionWhenEmpty: Boolean = false,
): Int? {
    if (movieIds.isEmpty()) {
        return if (keepSelectionWhenEmpty) selectedMovieId else null
    }
    if (selectedMovieId != null && selectedMovieId in movieIds) return selectedMovieId
    return movieIds.first()
}
