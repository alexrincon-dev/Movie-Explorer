package dev.alexrincon.movieclient.domain.error

sealed interface MovieError {
    data object Network : MovieError

    data object Server : MovieError

    data object RateLimited : MovieError

    data object Unauthorized : MovieError

    data object NotFound : MovieError

    data object InvalidResponse : MovieError

    data object Unknown : MovieError
}
