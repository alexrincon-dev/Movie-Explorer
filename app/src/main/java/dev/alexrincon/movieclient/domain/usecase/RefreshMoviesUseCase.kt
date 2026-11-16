package dev.alexrincon.movieclient.domain.usecase

import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.domain.repository.MovieRepository
import javax.inject.Inject

class RefreshMoviesUseCase @Inject constructor(
    private val movieRepository: MovieRepository,
) {
    suspend operator fun invoke(
        listType: MovieListType,
        language: MovieContentLanguage,
    ) {
        movieRepository.refreshMovies(listType, language)
    }
}
