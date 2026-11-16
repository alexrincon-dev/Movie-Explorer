package dev.alexrincon.movieclient.data.remote.mapper

import dev.alexrincon.movieclient.data.image.TmdbImageUrlBuilder
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.domain.model.Movie

fun MovieDto.toMovie(imageUrls: TmdbImageUrlBuilder): Movie {
    return Movie(
        id = id,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = imageUrls.posterUrl(posterPath),
        backdropPath = imageUrls.backdropUrl(backdropPath),
        releaseDate = releaseDate?.takeIf { it.isNotBlank() },
        voteAverage = voteAverage ?: 0.0,
    )
}
