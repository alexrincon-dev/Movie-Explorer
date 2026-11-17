@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.alexrincon.movieclient.presentation.movielist

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexrincon.movieclient.R
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.ui.theme.MovieClientTheme

private val movieListTypes = listOf(
    MovieListType.POPULAR to R.string.movie_list_popular,
    MovieListType.TOP_RATED to R.string.movie_list_top_rated,
)

@Composable
fun MovieListScreen(
    onMovieClick: (movieId: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedMovieId: Int? = null,
    viewModel: MovieListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MovieListContent(
        uiState = uiState,
        onMovieClick = onMovieClick,
        onListTypeSelected = viewModel::selectListType,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onLanguageSelected = viewModel::changeContentLanguage,
        selectedMovieId = selectedMovieId,
        modifier = modifier,
    )
}

@Composable
private fun MovieListContent(
    uiState: MovieListUiState,
    onMovieClick: (movieId: Int) -> Unit,
    onListTypeSelected: (MovieListType) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onLanguageSelected: (MovieContentLanguage) -> Unit,
    selectedMovieId: Int? = null,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MovieListTopBar(
                contentLanguage = uiState.contentLanguage,
                onLanguageSelected = onLanguageSelected,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MovieListTypeSelector(
                selectedListType = uiState.selectedListType,
                onListTypeSelected = onListTypeSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            MovieListBody(
                uiState = uiState,
                onMovieClick = onMovieClick,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                selectedMovieId = selectedMovieId,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MovieListTopBar(
    contentLanguage: MovieContentLanguage,
    onLanguageSelected: (MovieContentLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(text = stringResource(R.string.app_name))
        },
        actions = {
            ContentLanguageAction(
                contentLanguage = contentLanguage,
                onLanguageSelected = onLanguageSelected,
            )
        },
        colors = MovieListTopBarColors(),
    )
}

@Composable
private fun MovieListTopBarColors() = if (isSystemInDarkTheme()) {
    TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    )
} else {
    TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        titleContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionIconContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    )
}

@Composable
private fun ContentLanguageAction(
    contentLanguage: MovieContentLanguage,
    onLanguageSelected: (MovieContentLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val currentLanguageName = LanguageName(contentLanguage)
    val actionColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.inverseOnSurface
    }
    Box(modifier = modifier) {
        val actionDescription = stringResource(R.string.language_action, currentLanguageName)
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(contentColor = actionColor),
        ) {
            Text(
                text = LanguageCode(contentLanguage),
                modifier = Modifier.semantics { contentDescription = actionDescription },
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MovieContentLanguage.entries.forEach { language ->
                val selected = language == contentLanguage
                DropdownMenuItem(
                    text = {
                        Text(
                            text = LanguageName(language),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Unspecified
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onLanguageSelected(language)
                    },
                )
            }
        }
    }
}

@Composable
private fun MovieListTypeSelector(
    selectedListType: MovieListType,
    onListTypeSelected: (MovieListType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        movieListTypes.forEachIndexed { index, (listType, labelRes) ->
            SegmentedButton(
                selected = selectedListType == listType,
                onClick = { onListTypeSelected(listType) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = movieListTypes.size,
                ),
                icon = {},
                label = {
                    Text(text = stringResource(labelRes))
                },
            )
        }
    }
}

@Composable
private fun MovieListBody(
    uiState: MovieListUiState,
    onMovieClick: (movieId: Int) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    selectedMovieId: Int? = null,
    modifier: Modifier = Modifier,
) {
    val showLoading = uiState.movies.isEmpty() &&
        uiState.error == null &&
        (uiState.isInitialLoading || uiState.isRefreshing)
    val retry = {
        if (uiState.retriesFailedPage) onLoadMore() else onRefresh()
    }
    Box(modifier = modifier) {
        when {
            showLoading -> MovieListLoading(modifier = Modifier.fillMaxSize())
            uiState.movies.isEmpty() -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.error != null) {
                        MovieListMessage(
                            message = MovieErrorMessage(uiState.error),
                            actionLabel = stringResource(R.string.movie_list_retry),
                            onAction = retry,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        MovieListMessage(
                            message = stringResource(R.string.movie_list_empty),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    val error = uiState.error
                    if (error != null) {
                        MovieListErrorBanner(
                            message = MovieErrorMessage(error),
                            onRetry = retry,
                        )
                    }
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        MoviePosterGrid(
                            movies = uiState.movies,
                            listType = uiState.selectedListType,
                            isLoadingMore = uiState.isLoadingMore,
                            onMovieClick = onMovieClick,
                            onLoadMore = onLoadMore,
                            selectedMovieId = selectedMovieId,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieListLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.movie_list_loading)
    Column(
        modifier = modifier
            .padding(8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(MoviePosterAspectRatio)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieListMessage(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
private fun MovieListErrorBanner(
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

@Composable
private fun LanguageName(language: MovieContentLanguage): String {
    val nameRes = when (language) {
        MovieContentLanguage.EN_US -> R.string.language_english
        MovieContentLanguage.ES_MX -> R.string.language_spanish
    }
    return stringResource(nameRes)
}

@Composable
private fun LanguageCode(language: MovieContentLanguage): String {
    val codeRes = when (language) {
        MovieContentLanguage.EN_US -> R.string.language_english_code
        MovieContentLanguage.ES_MX -> R.string.language_spanish_code
    }
    return stringResource(codeRes)
}

@Preview(name = "Movies", showBackground = true, widthDp = 360, heightDp = 780)
@Preview(
    name = "Movies dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 780,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MovieListContentPreview() {
    MovieClientTheme {
        MovieListContent(
            uiState = MovieListUiState(
                movies = previewMovies(),
                selectedListType = MovieListType.POPULAR,
                contentLanguage = MovieContentLanguage.EN_US,
                isInitialLoading = false,
                canLoadMore = true,
            ),
            onMovieClick = {},
            onListTypeSelected = {},
            onRefresh = {},
            onLoadMore = {},
            onLanguageSelected = {},
        )
    }
}

@Preview(name = "Loading", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun MovieListLoadingPreview() {
    MovieClientTheme {
        MovieListContent(
            uiState = MovieListUiState(isInitialLoading = true),
            onMovieClick = {},
            onListTypeSelected = {},
            onRefresh = {},
            onLoadMore = {},
            onLanguageSelected = {},
        )
    }
}

@Preview(name = "Error", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun MovieListErrorPreview() {
    MovieClientTheme {
        MovieListContent(
            uiState = MovieListUiState(
                isInitialLoading = false,
                error = MovieError.Network,
            ),
            onMovieClick = {},
            onListTypeSelected = {},
            onRefresh = {},
            onLoadMore = {},
            onLanguageSelected = {},
        )
    }
}

@Preview(name = "Cached error", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun MovieListCachedErrorPreview() {
    MovieClientTheme {
        MovieListContent(
            uiState = MovieListUiState(
                movies = previewMovies(),
                isInitialLoading = false,
                error = MovieError.Server,
            ),
            onMovieClick = {},
            onListTypeSelected = {},
            onRefresh = {},
            onLoadMore = {},
            onLanguageSelected = {},
        )
    }
}

private fun previewMovies(): List<Movie> {
    return List(6) { index ->
        Movie(
            id = index + 1,
            originalTitle = "Movie ${index + 1}",
            overview = "",
            posterPath = null,
            backdropPath = null,
            releaseDate = null,
            voteAverage = 7.5,
        )
    }
}
