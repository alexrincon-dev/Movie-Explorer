package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.alexrincon.movieclient.presentation.moviedetail.MovieDetailScreen
import dev.alexrincon.movieclient.presentation.movielist.MovieListScreen
import dev.alexrincon.movieclient.presentation.movielist.rememberMovieGridStates

@Composable
fun MovieNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val selections = rememberMovieListSelections()
    var handoffMovieId by rememberSaveable { mutableStateOf<Int?>(null) }

    NavHost(
        navController = navController,
        startDestination = MovieListRoute,
        modifier = modifier.fillMaxSize(),
    ) {
        composable<MovieListRoute> { entry ->
            val gridStates = rememberMovieGridStates()
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (usesMasterDetailLayout(maxWidth)) {
                    MovieMasterDetail(
                        selections = selections,
                        handoffMovieId = handoffMovieId,
                        onHandoffConsumed = { handoffMovieId = null },
                        gridStates = gridStates,
                    )
                } else {
                    MovieListScreen(
                        onMovieClick = { listType, movieId ->
                            // Same guard as dropUnlessResumed, which only accepts lambdas without arguments.
                            if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
                                selections.select(listType, movieId)
                                navController.navigate(MovieDetailRoute(movieId = movieId))
                            }
                        },
                        gridStates = gridStates,
                    )
                }
            }
        }
        composable<MovieDetailRoute> { entry ->
            val movieId = entry.toRoute<MovieDetailRoute>().movieId
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (usesMasterDetailLayout(maxWidth)) {
                    LaunchedEffect(movieId) {
                        handoffMovieId = movieId
                        navController.popBackStack()
                    }
                } else {
                    MovieDetailScreen(
                        onBack = dropUnlessResumed { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
