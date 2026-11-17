package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexrincon.movieclient.R
import dev.alexrincon.movieclient.presentation.moviedetail.MovieDetailScreen
import dev.alexrincon.movieclient.presentation.moviedetail.MovieDetailViewModel
import dev.alexrincon.movieclient.presentation.movielist.MovieGridStates
import dev.alexrincon.movieclient.presentation.movielist.MovieListScreen
import dev.alexrincon.movieclient.presentation.movielist.MovieListViewModel

@Composable
internal fun MovieMasterDetail(
    selections: MovieListSelections,
    handoffMovieId: Int?,
    onHandoffConsumed: () -> Unit,
    gridStates: MovieGridStates,
    modifier: Modifier = Modifier,
    listViewModel: MovieListViewModel = hiltViewModel(),
    detailViewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val listState by listViewModel.uiState.collectAsStateWithLifecycle()
    val listType = listState.selectedListType
    val movieIds = listState.movies.map { movie -> movie.id }
    val storedId = selections.selectedId(listType)
    val requestedId = storedId ?: handoffMovieId
    val keepSelectionWhenEmpty = movieIds.isEmpty() &&
        (listState.isInitialLoading || listState.isRefreshing)
    val displayedMovieId = resolveSelectedMovieId(
        selectedMovieId = requestedId,
        movieIds = movieIds,
        keepSelectionWhenEmpty = keepSelectionWhenEmpty,
    )
    SideEffect {
        if (selections.selectedId(listType) != displayedMovieId) {
            selections.select(listType, displayedMovieId)
        }
        if (handoffMovieId != null) onHandoffConsumed()
    }
    LaunchedEffect(listType, displayedMovieId, movieIds.isEmpty(), keepSelectionWhenEmpty) {
        if (movieIds.isNotEmpty() && displayedMovieId != null) {
            detailViewModel.selectMovie(displayedMovieId)
        } else if (movieIds.isEmpty() && !keepSelectionWhenEmpty) {
            detailViewModel.clearSelectedMovie()
        }
    }
    DisposableEffect(detailViewModel) {
        onDispose { detailViewModel.clearSelectedMovie() }
    }

    Row(modifier = modifier.fillMaxSize()) {
        MovieListScreen(
            onMovieClick = { clickedList, movieId ->
                selections.select(clickedList, movieId)
            },
            gridStates = gridStates,
            selectedMovieId = displayedMovieId,
            viewModel = listViewModel,
            modifier = Modifier
                .weight(LIST_PANE_WEIGHT)
                .fillMaxSize(),
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(
            modifier = Modifier
                .weight(DETAIL_PANE_WEIGHT)
                .fillMaxSize(),
        ) {
            if (movieIds.isEmpty()) {
                MovieDetailPaneMessage(
                    message = stringResource(
                        if (listState.isInitialLoading || listState.isRefreshing) {
                            R.string.movie_detail_loading
                        } else {
                            R.string.movie_list_empty
                        },
                    ),
                )
            } else {
                MovieDetailScreen(
                    onBack = {},
                    showBackNavigation = false,
                    viewModel = detailViewModel,
                )
            }
        }
    }
}

@Composable
private fun MovieDetailPaneMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val LIST_PANE_WEIGHT = 2f
private const val DETAIL_PANE_WEIGHT = 3f
