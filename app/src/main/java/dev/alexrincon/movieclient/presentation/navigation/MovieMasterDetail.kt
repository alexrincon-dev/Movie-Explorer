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
import dev.alexrincon.movieclient.presentation.movielist.MovieListScreen
import dev.alexrincon.movieclient.presentation.movielist.MovieListViewModel

@Composable
internal fun MovieMasterDetail(
    selectedMovieId: Int?,
    onMovieSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    listViewModel: MovieListViewModel = hiltViewModel(),
    detailViewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val listState by listViewModel.uiState.collectAsStateWithLifecycle()
    val movieIds = listState.movies.map { movie -> movie.id }
    val displayedMovieId = resolveSelectedMovieId(selectedMovieId, movieIds)
    LaunchedEffect(displayedMovieId, movieIds.isEmpty()) {
        if (movieIds.isNotEmpty() && displayedMovieId != null && displayedMovieId != selectedMovieId) {
            onMovieSelected(displayedMovieId)
        }
    }
    LaunchedEffect(displayedMovieId, movieIds.isEmpty()) {
        if (movieIds.isNotEmpty() && displayedMovieId != null) {
            detailViewModel.selectMovie(displayedMovieId)
        }
    }
    DisposableEffect(detailViewModel) {
        onDispose { detailViewModel.clearSelectedMovie() }
    }

    Row(modifier = modifier.fillMaxSize()) {
        MovieListScreen(
            onMovieClick = onMovieSelected,
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
