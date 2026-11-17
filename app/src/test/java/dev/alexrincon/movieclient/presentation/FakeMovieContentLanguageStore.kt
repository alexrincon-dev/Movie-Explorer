package dev.alexrincon.movieclient.presentation

import dev.alexrincon.movieclient.domain.language.MovieContentLanguageStore
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage

internal class FakeMovieContentLanguageStore(
    initial: MovieContentLanguage = MovieContentLanguage.EN_US,
) : MovieContentLanguageStore {
    var stored: MovieContentLanguage = initial
    val writes = mutableListOf<MovieContentLanguage>()
    var readError: Exception? = null

    override suspend fun read(): MovieContentLanguage {
        readError?.let { throw it }
        return stored
    }

    override suspend fun write(language: MovieContentLanguage) {
        writes += language
        stored = language
    }
}
