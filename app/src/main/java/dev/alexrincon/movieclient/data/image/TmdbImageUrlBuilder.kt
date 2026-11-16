package dev.alexrincon.movieclient.data.image

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbImageUrlBuilder @Inject constructor() {
    fun posterUrl(path: String?): String? = imageUrl(path, POSTER_SIZE)

    fun backdropUrl(path: String?): String? = imageUrl(path, BACKDROP_SIZE)

    private fun imageUrl(path: String?, size: String): String? {
        val relativePath = path?.trim()?.trimStart('/').orEmpty()
        if (relativePath.isEmpty()) {
            return null
        }
        return IMAGE_BASE_URL + size + "/" + relativePath
    }

    private companion object {
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val POSTER_SIZE = "w500"
        const val BACKDROP_SIZE = "w780"
    }
}
