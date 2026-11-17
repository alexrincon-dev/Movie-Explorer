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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.alexrincon.movieclient.presentation.moviedetail.MovieDetailScreen
import dev.alexrincon.movieclient.presentation.movielist.MovieListScreen

@Composable
fun MovieNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    var selectedMovieId by rememberSaveable { mutableStateOf<Int?>(null) }

    NavHost(
        navController = navController,
        startDestination = MovieListRoute,
        modifier = modifier.fillMaxSize(),
    ) {
        composable<MovieListRoute> {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (usesMasterDetailLayout(maxWidth)) {
                    MovieMasterDetail(
                        selectedMovieId = selectedMovieId,
                        onMovieSelected = { movieId -> selectedMovieId = movieId },
                    )
                } else {
                    MovieListScreen(
                        onMovieClick = { movieId ->
                            selectedMovieId = movieId
                            navController.navigate(MovieDetailRoute(movieId = movieId))
                        },
                    )
                }
            }
        }
        composable<MovieDetailRoute> { entry ->
            val movieId = entry.toRoute<MovieDetailRoute>().movieId
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if (usesMasterDetailLayout(maxWidth)) {
                    LaunchedEffect(movieId) {
                        selectedMovieId = movieId
                        navController.popBackStack()
                    }
                } else {
                    MovieDetailScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
