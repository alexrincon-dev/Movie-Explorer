package dev.alexrincon.movieclient.domain.language

import dev.alexrincon.movieclient.domain.model.MovieContentLanguage

/**
 * Persisted TMDB content language.
 * The storage implementation stays in the data layer.
 */
interface MovieContentLanguageStore {
    suspend fun read(): MovieContentLanguage

    suspend fun write(language: MovieContentLanguage)
}
