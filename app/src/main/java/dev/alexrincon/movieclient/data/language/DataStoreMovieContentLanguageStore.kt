package dev.alexrincon.movieclient.data.language

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.alexrincon.movieclient.domain.language.MovieContentLanguageStore
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import kotlinx.coroutines.flow.first

internal val MovieContentLanguageKey = stringPreferencesKey("content_language")

/**
 * A missing or unrecognized value falls back to [MovieContentLanguage.EN_US].
 * Stored values are the domain enum names, not TMDB language tags.
 */
internal class DataStoreMovieContentLanguageStore(
    private val dataStore: DataStore<Preferences>,
) : MovieContentLanguageStore {
    override suspend fun read(): MovieContentLanguage {
        val stored = dataStore.data.first()[MovieContentLanguageKey]
        return stored.toMovieContentLanguage()
    }

    override suspend fun write(language: MovieContentLanguage) {
        dataStore.edit { preferences ->
            preferences[MovieContentLanguageKey] = language.name
        }
    }
}

internal fun String?.toMovieContentLanguage(): MovieContentLanguage {
    if (this == null) return MovieContentLanguage.EN_US
    return MovieContentLanguage.entries.firstOrNull { it.name == this }
        ?: MovieContentLanguage.EN_US
}
