package dev.alexrincon.movieclient.presentation

import dev.alexrincon.movieclient.domain.language.MovieContentLanguageStore
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import kotlinx.coroutines.CompletableDeferred

internal class FakeMovieContentLanguageStore(
    initial: MovieContentLanguage = MovieContentLanguage.EN_US,
) : MovieContentLanguageStore {
    var stored: MovieContentLanguage = initial
    val writes = mutableListOf<MovieContentLanguage>()
    var readError: Exception? = null
    var writeError: Exception? = null
    var writeGate: CompletableDeferred<Unit>? = null

    override suspend fun read(): MovieContentLanguage {
        readError?.let { throw it }
        return stored
    }

    /** Only successful writes are recorded in [writes]. */
    override suspend fun write(language: MovieContentLanguage) {
        writeGate?.await()
        writeError?.let { throw it }
        writes += language
        stored = language
    }
}
