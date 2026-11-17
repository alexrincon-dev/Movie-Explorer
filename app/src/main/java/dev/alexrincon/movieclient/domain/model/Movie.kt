package dev.alexrincon.movieclient.domain.model

data class Movie(
    val id: Int,
    /** Title returned for the requested content language. */
    val title: String,
    /** Title in the movie's original language. Independent of [title]. */
    val originalTitle: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
)
