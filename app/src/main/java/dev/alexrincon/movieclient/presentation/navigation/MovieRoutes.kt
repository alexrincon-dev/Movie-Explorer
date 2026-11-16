package dev.alexrincon.movieclient.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Start destination. Popular and Top Rated lists will be shown here.
 */
@Serializable
data object MovieListRoute

/**
 * Detail destination. Only [movieId] is a navigation argument.
 * The detail screen loads the movie from that id.
 */
@Serializable
data class MovieDetailRoute(
    val movieId: Int,
)
