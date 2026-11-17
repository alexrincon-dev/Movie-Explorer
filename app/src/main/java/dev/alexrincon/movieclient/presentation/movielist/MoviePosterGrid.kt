package dev.alexrincon.movieclient.presentation.movielist

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import dev.alexrincon.movieclient.R
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.presentation.displayTitle
import dev.alexrincon.movieclient.domain.model.MovieListType

internal const val MoviePosterAspectRatio = 2f / 3f

private const val LoadMorePrefetchItems = 4
private const val LoadingMoreKey = Int.MIN_VALUE

/**
 * True when the last visible row is within [LoadMorePrefetchItems] of the end
 * and no refresh or page request is already blocking pagination.
 * [movieCount] is part of the result so a new list is evaluated again.
 */
internal fun movieGridShouldLoadMore(
    lastVisibleIndex: Int?,
    totalItems: Int,
    movieCount: Int,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
): Boolean {
    if (isLoadingMore || isRefreshing) return false
    return movieGridIsNearEnd(lastVisibleIndex, totalItems, movieCount)
}

/** True when the last visible row is within [LoadMorePrefetchItems] of the end, ignoring loading state. */
internal fun movieGridIsNearEnd(
    lastVisibleIndex: Int?,
    totalItems: Int,
    movieCount: Int,
): Boolean {
    if (movieCount <= 0 || lastVisibleIndex == null || totalItems <= 0) return false
    return lastVisibleIndex >= totalItems - LoadMorePrefetchItems
}

internal data class MovieGridLoadMoreSignal(
    val isNearEnd: Boolean,
    val shouldLoadMore: Boolean,
    val paginationFailed: Boolean,
)

/**
 * Decides whether a [MovieGridLoadMoreSignal] requests the next page.
 * After a failed page, the grid does not request it again on its own, even when loading ends
 * while the grid is still at the end. The user must tap Retry, which does not go through this
 * trigger, or scroll out of the prefetch window and back.
 * Each scroll out and back allows one request, so the trigger does not depend on the ViewModel
 * updating its state before the next signal.
 * A successful page or a refresh clears the failure and restores automatic pagination.
 */
internal class MovieGridLoadMoreTrigger {
    private var retryArmed = false

    fun shouldRequest(signal: MovieGridLoadMoreSignal): Boolean {
        if (!signal.paginationFailed) {
            retryArmed = false
            return signal.shouldLoadMore
        }
        if (!signal.isNearEnd) {
            retryArmed = true
            return false
        }
        if (!signal.shouldLoadMore || !retryArmed) return false
        retryArmed = false
        return true
    }
}

/**
 * Scroll position of each movie list. Popular and Top Rated keep independent positions.
 * Create it above the phone/tablet layout switch so a resize does not reset the positions.
 */
@Stable
class MovieGridStates internal constructor(
    private val popular: LazyGridState,
    private val topRated: LazyGridState,
) {
    operator fun get(listType: MovieListType): LazyGridState = when (listType) {
        MovieListType.POPULAR -> popular
        MovieListType.TOP_RATED -> topRated
    }
}

@Composable
fun rememberMovieGridStates(): MovieGridStates {
    val popular = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val topRated = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    return remember(popular, topRated) { MovieGridStates(popular, topRated) }
}

@Composable
internal fun MoviePosterGrid(
    movies: List<Movie>,
    gridState: LazyGridState,
    isLoadingMore: Boolean,
    onMovieClick: (movieId: Int) -> Unit,
    onLoadMore: () -> Unit,
    isRefreshing: Boolean = false,
    paginationFailed: Boolean = false,
    selectedMovieId: Int? = null,
    modifier: Modifier = Modifier,
) {
    val movieCount = rememberUpdatedState(movies.size)
    val loadingMore = rememberUpdatedState(isLoadingMore)
    val refreshing = rememberUpdatedState(isRefreshing)
    val failed = rememberUpdatedState(paginationFailed)
    val loadMoreRequest = rememberUpdatedState(onLoadMore)
    LaunchedEffect(gridState) {
        val trigger = MovieGridLoadMoreTrigger()
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            MovieGridLoadMoreSignal(
                isNearEnd = movieGridIsNearEnd(
                    lastVisibleIndex = lastVisibleIndex,
                    totalItems = layoutInfo.totalItemsCount,
                    movieCount = movieCount.value,
                ),
                shouldLoadMore = movieGridShouldLoadMore(
                    lastVisibleIndex = lastVisibleIndex,
                    totalItems = layoutInfo.totalItemsCount,
                    movieCount = movieCount.value,
                    isLoadingMore = loadingMore.value,
                    isRefreshing = refreshing.value,
                ),
                paginationFailed = failed.value,
            )
        }.collect { signal ->
            if (trigger.shouldRequest(signal)) loadMoreRequest.value()
        }
    }

    val loadingDescription = stringResource(R.string.movie_list_loading)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = movies,
            key = { movie -> movie.id },
            contentType = { "poster" },
        ) { movie ->
            MoviePoster(
                movie = movie,
                selected = selectedMovieId == movie.id,
                showSelection = selectedMovieId != null,
                onClick = { onMovieClick(movie.id) },
            )
        }
        if (isLoadingMore) {
            item(
                key = LoadingMoreKey,
                span = { GridItemSpan(maxLineSpan) },
                contentType = "loading",
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .semantics { contentDescription = loadingDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun MoviePoster(
    movie: Movie,
    selected: Boolean,
    showSelection: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = movie.displayTitle()
    val description = stringResource(R.string.movie_poster_content_description, title)
    val shape = MaterialTheme.shapes.small
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showSelection) {
                    Modifier.border(
                        width = 3.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MoviePosterAspectRatio)
                .semantics {
                    contentDescription = description
                    if (showSelection) this.selected = selected
                },
        ) {
            val posterUrl = movie.posterPath
            if (posterUrl.isNullOrBlank()) {
                PosterFallback(title = title)
            } else {
                SubcomposeAsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) {
                    val imageState by painter.state.collectAsState()
                    if (imageState is AsyncImagePainter.State.Error) {
                        PosterFallback(title = title)
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterFallback(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
