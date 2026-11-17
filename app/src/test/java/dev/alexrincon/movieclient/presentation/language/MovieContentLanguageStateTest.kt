package dev.alexrincon.movieclient.presentation.language

import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.presentation.FakeMovieContentLanguageStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MovieContentLanguageStateTest {
    @Test
    fun readFailureFallsBackToEnglish() = runBlocking {
        val store = FakeMovieContentLanguageStore().apply {
            readError = IOException("unreadable")
        }
        val state = MovieContentLanguageState(store)

        assertEquals(MovieContentLanguage.EN_US, state.awaitLanguage())
        assertEquals(MovieContentLanguage.EN_US, state.contentLanguage.value)
    }

    @Test
    fun updateIsNotReplacedByAnOlderRead() = runBlocking {
        val store = FakeMovieContentLanguageStore(MovieContentLanguage.EN_US)
        val state = MovieContentLanguageState(store)

        assertNull(state.contentLanguage.value)
        state.updateContentLanguage(MovieContentLanguage.ES_MX)

        assertEquals(MovieContentLanguage.ES_MX, state.awaitLanguage())
        assertEquals(MovieContentLanguage.ES_MX, state.contentLanguage.value)
        assertEquals(listOf(MovieContentLanguage.ES_MX), store.writes)
        assertTrue(store.stored == MovieContentLanguage.ES_MX)
    }

    @Test
    fun successfulWritePublishesTheLanguage() = runBlocking {
        val store = FakeMovieContentLanguageStore(MovieContentLanguage.EN_US)
        val state = MovieContentLanguageState(store)
        state.awaitLanguage()

        state.updateContentLanguage(MovieContentLanguage.ES_MX)

        assertEquals(MovieContentLanguage.ES_MX, state.contentLanguage.value)
        assertEquals(listOf(MovieContentLanguage.ES_MX), store.writes)
    }

    @Test
    fun writeFailureIsThrownAndKeepsThePublishedLanguage() = runBlocking {
        val failure = IOException("disk full")
        val store = FakeMovieContentLanguageStore(MovieContentLanguage.EN_US)
        val state = MovieContentLanguageState(store)
        state.awaitLanguage()
        store.writeError = failure

        val thrown = assertThrows(IOException::class.java) {
            runBlocking { state.updateContentLanguage(MovieContentLanguage.ES_MX) }
        }

        assertSame(failure, thrown)
        assertEquals(MovieContentLanguage.EN_US, state.contentLanguage.value)
        assertEquals(MovieContentLanguage.EN_US, store.stored)
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun cancelledWriteIsRethrownAndKeepsThePublishedLanguage() = runBlocking {
        val cancellation = CancellationException("stop")
        val store = FakeMovieContentLanguageStore(MovieContentLanguage.EN_US)
        val state = MovieContentLanguageState(store)
        state.awaitLanguage()
        store.writeError = cancellation

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { state.updateContentLanguage(MovieContentLanguage.ES_MX) }
        }

        assertSame(cancellation, thrown)
        assertEquals(MovieContentLanguage.EN_US, state.contentLanguage.value)
        assertTrue(store.writes.isEmpty())
    }
}
