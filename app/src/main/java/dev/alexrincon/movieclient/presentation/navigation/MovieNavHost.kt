package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
        composable<MovieDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MovieDetailRoute>()
            MovieDetailPlaceholder(
                movieId = route.movieId,
                onBack = { navController.popBackStack() },
                modifier = Modifier.safeDrawingPadding(),
            )
        }
    }
}
