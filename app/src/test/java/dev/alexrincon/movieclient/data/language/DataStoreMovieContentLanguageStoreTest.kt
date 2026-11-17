package dev.alexrincon.movieclient.data.language

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DataStoreMovieContentLanguageStoreTest {
    @Test
    fun missingAndInvalidValuesFallBackToEnglish() {
        assertEquals(MovieContentLanguage.EN_US, null.toMovieContentLanguage())
        assertEquals(MovieContentLanguage.EN_US, "".toMovieContentLanguage())
        assertEquals(MovieContentLanguage.EN_US, "en-US".toMovieContentLanguage())
        assertEquals(MovieContentLanguage.EN_US, "es-MX".toMovieContentLanguage())
        assertEquals(MovieContentLanguage.EN_US, "FR_FR".toMovieContentLanguage())
        assertEquals(MovieContentLanguage.EN_US, "ES_MX ".toMovieContentLanguage())
        assertEquals(MovieContentLanguage.EN_US, "EN_US".toMovieContentLanguage())
        assertEquals(MovieContentLanguage.ES_MX, "ES_MX".toMovieContentLanguage())
    }

    @Test
    fun read_returnsEnglishWhenNothingIsStored() = runBlocking {
        val store = newStore()

        assertEquals(MovieContentLanguage.EN_US, store.read())
    }

    @Test
    fun writeThenReadRestoresEnglishAndSpanish() = runBlocking {
        val store = newStore()

        store.write(MovieContentLanguage.ES_MX)
        assertEquals(MovieContentLanguage.ES_MX, store.read())

        store.write(MovieContentLanguage.EN_US)
        assertEquals(MovieContentLanguage.EN_US, store.read())
    }

    @Test
    fun read_returnsEnglishWhenTheStoredValueIsInvalid() = runBlocking {
        val directory = File(System.getProperty("java.io.tmpdir"), "movie-language-${System.nanoTime()}")
        directory.mkdirs()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { File(directory, "movie_content_language.preferences_pb") },
        )
        val store = DataStoreMovieContentLanguageStore(dataStore)
        dataStore.edit { preferences ->
            preferences[MovieContentLanguageKey] = "not-a-language"
        }

        assertEquals(MovieContentLanguage.EN_US, store.read())
    }

    private fun newStore(): DataStoreMovieContentLanguageStore {
        val directory = File(System.getProperty("java.io.tmpdir"), "movie-language-${System.nanoTime()}")
        directory.mkdirs()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { File(directory, "movie_content_language.preferences_pb") },
        )
        return DataStoreMovieContentLanguageStore(dataStore)
    }
}
