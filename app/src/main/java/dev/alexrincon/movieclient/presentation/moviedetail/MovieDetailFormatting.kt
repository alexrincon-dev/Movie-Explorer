package dev.alexrincon.movieclient.presentation.moviedetail

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun movieReleaseYear(releaseDate: String?): Int? {
    parseReleaseDate(releaseDate)?.let { return it.year }
    return releaseDate?.trim()?.take(4)?.toIntOrNull()
}

internal fun movieReleaseDateLabel(
    releaseDate: String?,
    locale: Locale = Locale.getDefault(),
): String? {
    val parsed = parseReleaseDate(releaseDate)
    if (parsed != null) {
        return parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    return releaseDate?.trim()?.takeIf { it.isNotEmpty() }
}

private fun parseReleaseDate(releaseDate: String?): LocalDate? {
    val value = releaseDate?.trim().orEmpty()
    if (value.isEmpty()) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}
