package dev.alexrincon.movieclient.presentation

import dev.alexrincon.movieclient.domain.model.Movie

/** Localized title, or the original title when the localized value is blank. */
internal fun Movie.displayTitle(): String = title.trim().ifEmpty { originalTitle.trim() }

/**
 * Original title when it is present and different from the localized title.
 * A blank localized title is already shown through [displayTitle], so it is not repeated.
 */
internal fun Movie.distinctOriginalTitle(): String? {
    val localized = title.trim()
    val original = originalTitle.trim()
    if (localized.isEmpty() || original.isEmpty() || localized == original) {
        return null
    }
    return original
}
