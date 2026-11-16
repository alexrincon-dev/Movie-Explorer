package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.alexrincon.movieclient.presentation.moviedetail.MovieDetailScreen
import dev.alexrincon.movieclient.presentation.movielist.MovieListScreen

@Composable
fun MovieNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MovieListRoute,
        modifier = modifier.fillMaxSize(),
    ) {
        composable<MovieListRoute> {
            MovieListScreen(
                onMovieClick = { movieId ->
                    navController.navigate(MovieDetailRoute(movieId = movieId))
                },
            )
        }
        composable<MovieDetailRoute> {
            MovieDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
