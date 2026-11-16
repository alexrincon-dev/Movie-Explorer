package dev.alexrincon.movieclient.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun MovieNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MovieListRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MovieListRoute> {
                MovieListPlaceholder(
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
                )
            }
        }
    }
}
