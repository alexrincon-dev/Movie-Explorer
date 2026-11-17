@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.alexrincon.movieclient.presentation.moviedetail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import dev.alexrincon.movieclient.R
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.presentation.movielist.MoviePosterAspectRatio
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme

@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBackNavigation: Boolean = true,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MovieDetailContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
        showBackNavigation = showBackNavigation,
        modifier = modifier,
    )
}

@Composable
private fun MovieDetailContent(
    uiState: MovieDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    showBackNavigation: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MovieDetailTopBar(
                onBack = onBack,
                showBackNavigation = showBackNavigation,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val movie = uiState.movie
        when {
            uiState.isLoading -> MovieDetailLoading(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            movie == null -> MovieDetailMessage(
                message = if (uiState.error != null) {
                    MovieErrorMessage(uiState.error)
                } else {
                    stringResource(R.string.movie_detail_unavailable)
                },
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                val error = uiState.error
                if (error != null) {
                    MovieDetailErrorBanner(
                        message = MovieErrorMessage(error),
                        onRetry = onRetry,
                    )
                }
                MovieDetailBody(movie = movie)
            }
        }
    }
}

@Composable
private fun MovieDetailTopBar(
    onBack: () -> Unit,
    showBackNavigation: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.inverseOnSurface
    }
    val containerColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.inverseSurface
    }
    TopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            if (showBackNavigation) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.movie_detail_back),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            navigationIconContentColor = contentColor,
        ),
    )
}

@Composable
private fun MovieDetailBody(
    movie: Movie,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        val backdropUrl = movie.backdropPath
        if (!backdropUrl.isNullOrBlank()) {
            DetailImage(
                url = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
        }
        Text(
            text = movie.originalTitle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailPoster(
                title = movie.originalTitle,
                posterUrl = movie.posterPath,
                modifier = Modifier.width(124.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailFact(
                    label = stringResource(R.string.movie_detail_year),
                    value = movieReleaseYear(movie.releaseDate)?.toString()
                        ?: stringResource(R.string.movie_detail_year_missing),
                )
                DetailFact(
                    label = stringResource(R.string.movie_detail_rating),
                    value = stringResource(R.string.movie_detail_rating_value, movie.voteAverage),
                    valueColor = MaterialTheme.colorScheme.tertiary,
                )
                DetailFact(
                    label = stringResource(R.string.movie_detail_release_date),
                    value = movieReleaseDateLabel(movie.releaseDate, locale)
                        ?: stringResource(R.string.movie_detail_release_date_missing),
                )
            }
        }
        Text(
            text = stringResource(R.string.movie_detail_overview),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = movie.overview.ifBlank { stringResource(R.string.movie_detail_overview_missing) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (movie.overview.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun DetailFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}

@Composable
private fun DetailPoster(
    title: String,
    posterUrl: String?,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.movie_poster_content_description, title)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(MoviePosterAspectRatio)
                .semantics { contentDescription = description },
        ) {
            if (posterUrl.isNullOrBlank()) {
                PosterFallback(title = title)
            } else {
                DetailImage(
                    url = posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    fallback = { PosterFallback(title = title) },
                )
            }
        }
    }
}

@Composable
private fun DetailImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {},
) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    ) {
        val imageState by painter.state.collectAsState()
        when (imageState) {
            is AsyncImagePainter.State.Error -> fallback()
            else -> SubcomposeAsyncImageContent()
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

@Composable
private fun MovieDetailLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.movie_detail_loading)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(124.dp)
                .aspectRatio(MoviePosterAspectRatio)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
    }
}

@Composable
private fun MovieDetailMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = stringResource(R.string.movie_list_retry))
        }
    }
}

@Composable
private fun MovieDetailErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.movie_list_retry))
            }
        }
    }
}

@Composable
private fun MovieErrorMessage(error: MovieError): String {
    val messageRes = when (error) {
        MovieError.Network -> R.string.movie_list_error_network
        MovieError.Server -> R.string.movie_list_error_server
        MovieError.RateLimited -> R.string.movie_list_error_rate_limited
        MovieError.NotFound -> R.string.movie_list_error_not_found
        MovieError.InvalidResponse -> R.string.movie_list_error_invalid_response
        MovieError.Unknown -> R.string.movie_list_error_unknown
    }
    return stringResource(messageRes)
}

@Preview(name = "Detail", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(
    name = "Detail dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MovieDetailContentPreview() {
    MovieClientTheme {
        MovieDetailContent(
            uiState = MovieDetailUiState(
                movie = previewMovie(),
                isLoading = false,
            ),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Detail loading", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun MovieDetailLoadingPreview() {
    MovieClientTheme {
        MovieDetailContent(
            uiState = MovieDetailUiState(isLoading = true),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Detail cached error", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun MovieDetailCachedErrorPreview() {
    MovieClientTheme {
        MovieDetailContent(
            uiState = MovieDetailUiState(
                movie = previewMovie(posterPath = null, overview = ""),
                error = MovieError.Network,
            ),
            onBack = {},
            onRetry = {},
        )
    }
}

private fun previewMovie(
    posterPath: String? = null,
    overview: String = "A thief who steals secrets through shared dreaming.",
): Movie {
    return Movie(
        id = 1,
        originalTitle = "Inception",
        overview = overview,
        posterPath = posterPath,
        backdropPath = null,
        releaseDate = "2010-07-16",
        voteAverage = 8.4,
    )
}
