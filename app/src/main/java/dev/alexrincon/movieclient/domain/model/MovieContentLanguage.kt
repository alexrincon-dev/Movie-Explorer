package dev.alexrincon.movieclient.domain.model

/**
 * TMDB content language for movie text. UI translations stay in Android resources.
 * Data maps these values to the `en-US` and `es-MX` request parameters.
 */
enum class MovieContentLanguage {
    EN_US,
    ES_MX,
}
