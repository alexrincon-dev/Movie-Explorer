package dev.alexrincon.movieclient.data.local.mapper

import dev.alexrincon.movieclient.data.image.TmdbImageUrlBuilder
import dev.alexrincon.movieclient.data.local.entity.MovieEntity
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage

/**
 * Merge rules for a remote movie into the cached row:
 * - A missing poster, backdrop, or release date keeps the cached non-null value.
 * - A blank overview keeps a non-empty cached overview only when the language is unchanged.
 * - A language change stores the incoming overview, including an empty one, and that language tag.
 * - A blank localized title keeps a non-empty cached title only when the language is unchanged.
 * - A language change stores the incoming localized title, including an empty one.
 * - A blank original title keeps the cached original title and is never replaced by the localized title.
 * - A non-null voteAverage wins, including 0.0. A missing rating keeps the cached rating.
 * Room receives relative image paths. Domain movies receive Coil URLs.
 */
internal fun MovieDto.toEntity(
    language: MovieContentLanguage,
    existing: MovieEntity? = null,
): MovieEntity {
    val languageChanged = existing != null && existing.contentLanguage != language
    val incomingOverview = overview.trim()
    val cachedOverview = existing?.overview
    val storedOverview = when {
        languageChanged -> incomingOverview
        incomingOverview.isNotEmpty() -> incomingOverview
        !cachedOverview.isNullOrBlank() -> cachedOverview
        else -> incomingOverview
    }
    val incomingTitle = title.trim()
    val cachedTitle = existing?.title
    val storedTitle = when {
        languageChanged -> incomingTitle
        incomingTitle.isNotEmpty() -> incomingTitle
        !cachedTitle.isNullOrBlank() -> cachedTitle
        else -> incomingTitle
    }
    return MovieEntity(
        id = id,
        originalTitle = originalTitle.trim().ifEmpty { existing?.originalTitle.orEmpty() },
        overview = storedOverview,
        posterPath = posterPath.toRelativeImagePath() ?: existing?.posterPath,
        backdropPath = backdropPath.toRelativeImagePath() ?: existing?.backdropPath,
        releaseDate = releaseDate?.trim()?.takeIf { it.isNotEmpty() } ?: existing?.releaseDate,
        voteAverage = voteAverage ?: existing?.voteAverage ?: 0.0,
        contentLanguage = language,
        title = storedTitle,
    )
}

internal fun MovieEntity.toMovie(imageUrls: TmdbImageUrlBuilder): Movie {
    return Movie(
        id = id,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        posterPath = imageUrls.posterUrl(posterPath),
        backdropPath = imageUrls.backdropUrl(backdropPath),
        releaseDate = releaseDate?.takeIf { it.isNotBlank() },
        voteAverage = voteAverage,
    )
}

private fun String?.toRelativeImagePath(): String? {
    return this?.trim()?.trimStart('/')?.takeIf { it.isNotEmpty() }
}
