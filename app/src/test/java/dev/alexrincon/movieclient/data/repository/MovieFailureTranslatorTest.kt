package dev.alexrincon.movieclient.data.repository

import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
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

    private fun http(code: Int): HttpException {
        return HttpException(
            Response.error<String>(
                code,
                "".toResponseBody("text/plain".toMediaType()),
            ),
        )
    }
}
