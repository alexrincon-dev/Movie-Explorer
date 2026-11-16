package dev.alexrincon.movieclient.presentation.language

import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory TMDB content language for the whole app.
 * This is not the Android UI locale and it is not persisted.
 */
@Singleton
class MovieContentLanguageState @Inject constructor() {
    private val currentLanguage = MutableStateFlow(MovieContentLanguage.EN_US)

    val contentLanguage: StateFlow<MovieContentLanguage> = currentLanguage.asStateFlow()

    fun updateContentLanguage(language: MovieContentLanguage) {
        currentLanguage.value = language
    }
}
