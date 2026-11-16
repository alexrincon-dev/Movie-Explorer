package dev.alexrincon.movieclient.domain.error

class MovieException(
    val error: MovieError,
) : RuntimeException(error.toString())
