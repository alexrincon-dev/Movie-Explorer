package dev.alexrincon.movieclient.data.repository

import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class MovieFailureTranslatorTest {
    @Test
    fun mapsKnownFailures() {
        assertEquals(MovieError.Network, IOException("offline").toMovieException().error)
        assertEquals(MovieError.NotFound, http(404).toMovieException().error)
        assertEquals(MovieError.RateLimited, http(429).toMovieException().error)
        assertEquals(MovieError.Server, http(503).toMovieException().error)
        assertEquals(MovieError.InvalidResponse, SerializationException("bad json").toMovieException().error)
        assertEquals(MovieError.Unknown, IllegalStateException("boom").toMovieException().error)
    }

    @Test
    fun mapsUnauthorizedOnlyFrom401() {
        assertEquals(MovieError.Unauthorized, http(401).toMovieException().error)
        assertEquals(MovieError.Unauthorized, IOException("wrapped", http(401)).toMovieException().error)
        assertEquals(MovieError.Unknown, http(400).toMovieException().error)
        assertEquals(MovieError.Unknown, http(403).toMovieException().error)
    }

    @Test
    fun unauthorizedDoesNotCarryResponseDetails() {
        val exception = http(401, body = """{"status_message":"Invalid API key: secret-key"}""")
            .toMovieException()

        assertEquals(MovieError.Unauthorized, exception.error)
        assertNull(exception.cause)
        assertFalse(exception.message.orEmpty().contains("secret-key"))
    }

    @Test
    fun translateMovieFailures_wrapsUnauthorizedHttpException() = runBlocking {
        try {
            translateMovieFailures<Unit> { throw http(401) }
            fail()
        } catch (thrown: MovieException) {
            assertEquals(MovieError.Unauthorized, thrown.error)
        }
    }

    @Test
    fun translateMovieFailures_rethrowsCancellation() = runBlocking {
        val cancellation = CancellationException("stop")
        try {
            translateMovieFailures { throw cancellation }
            fail()
        } catch (thrown: CancellationException) {
            assertSame(cancellation, thrown)
        }
    }

    @Test
    fun translateMovieFailures_wrapsIoException() = runBlocking {
        try {
            translateMovieFailures<Unit> { throw IOException("offline") }
            fail()
        } catch (thrown: MovieException) {
            assertEquals(MovieError.Network, thrown.error)
        }
    }

    private fun http(code: Int, body: String = ""): HttpException {
        return HttpException(
            Response.error<String>(
                code,
                body.toResponseBody("text/plain".toMediaType()),
            ),
        )
    }
}
