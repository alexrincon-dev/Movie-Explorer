package dev.alexrincon.movieclient.data.repository

import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

internal suspend fun <T> translateMovieFailures(block: suspend () -> T): T {
    try {
        return block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: MovieException) {
        throw exception
    } catch (exception: Exception) {
        throw exception.toMovieException()
    }
}

internal fun Throwable.toMovieException(): MovieException {
    val httpException = causeChain().filterIsInstance<HttpException>().firstOrNull()
    if (httpException != null) {
        val error = when (httpException.code()) {
            HTTP_NOT_FOUND -> MovieError.NotFound
            HTTP_TOO_MANY_REQUESTS -> MovieError.RateLimited
            in HTTP_SERVER_ERROR_START..HTTP_SERVER_ERROR_END -> MovieError.Server
            else -> MovieError.Unknown
        }
        return MovieException(error)
    }
    if (causeChain().any { it is SerializationException }) {
        return MovieException(MovieError.InvalidResponse)
    }
    if (causeChain().any { it is IOException }) {
        return MovieException(MovieError.Network)
    }
    return MovieException(MovieError.Unknown)
}

private fun Throwable.causeChain(): Sequence<Throwable> = sequence {
    var current: Throwable? = this@causeChain
    val seen = HashSet<Throwable>()
    while (current != null && seen.add(current)) {
        yield(current)
        current = current.cause
    }
}

private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR_START = 500
private const val HTTP_SERVER_ERROR_END = 599
