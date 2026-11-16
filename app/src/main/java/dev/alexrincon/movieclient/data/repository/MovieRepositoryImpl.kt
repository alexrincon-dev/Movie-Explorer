package dev.alexrincon.movieclient.data.repository

import androidx.room.withTransaction
import dev.alexrincon.movieclient.data.image.TmdbImageUrlBuilder
import dev.alexrincon.movieclient.data.local.dao.MovieDao
import dev.alexrincon.movieclient.data.local.dao.MovieListDao
import dev.alexrincon.movieclient.data.local.dao.MovieListPaginationDao
import dev.alexrincon.movieclient.data.local.database.MovieDatabase
import dev.alexrincon.movieclient.data.local.entity.MovieEntity
import dev.alexrincon.movieclient.data.local.entity.MovieListEntity
import dev.alexrincon.movieclient.data.local.entity.MovieListPaginationEntity
import dev.alexrincon.movieclient.data.local.mapper.toEntity
import dev.alexrincon.movieclient.data.local.mapper.toMovie
import dev.alexrincon.movieclient.data.remote.datasource.TmdbRemoteDataSource
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.data.remote.dto.MoviePageDto
import dev.alexrincon.movieclient.domain.model.Movie
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListSnapshot
import dev.alexrincon.movieclient.domain.model.MovieListType
import dev.alexrincon.movieclient.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lock order, always outer to inner:
 * 1. Per-movie detail mutex, only around one detail request, including its network call.
 * 2. List mutexes, Popular then Top Rated, only around that list's network call and write.
 * 3. Language-generation mutex, only around a committed [refreshAllMovies] transaction and its
 *    generation increment, or around a detail generation check and its write.
 * 4. The Room transaction.
 *
 * The generation mutex is never held during a network call. Detail requests do not take list locks,
 * and list refreshes do not take detail locks, so those paths cannot deadlock.
 * [refreshAllMovies] is the language change for both lists. A failed call does not advance the generation.
 */
@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val database: MovieDatabase,
    private val movieDao: MovieDao,
    private val movieListDao: MovieListDao,
    private val paginationDao: MovieListPaginationDao,
    private val remote: TmdbRemoteDataSource,
    private val imageUrls: TmdbImageUrlBuilder,
) : MovieRepository {

    private val listLocks = MovieListType.entries.associateWith { Mutex() }
    private val languageGenerationLock = Mutex()
    private var languageGeneration = 0L
    private val detailLocks = ConcurrentHashMap<Int, DetailLock>()

    override fun observeMovies(listType: MovieListType): Flow<MovieListSnapshot> {
        return combine(
            movieDao.observeByListType(listType),
            paginationDao.observe(listType),
        ) { movies, pagination ->
            if (pagination == null && movies.isEmpty()) {
                MovieListSnapshot.Empty
            } else {
                MovieListSnapshot(
                    movies = movies.map { it.toMovie(imageUrls) },
                    lastLoadedPage = pagination?.lastLoadedPage ?: 0,
                    totalPages = pagination?.totalPages ?: 0,
                )
            }
        }.translateFlowFailures()
    }

    override fun observeMovie(movieId: Int): Flow<Movie?> {
        return movieDao.observeById(movieId)
            .map { entity -> entity?.toMovie(imageUrls) }
            .translateFlowFailures()
    }

    override suspend fun refreshMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    ) {
        translateMovieFailures {
            listLocks.getValue(listType).withLock {
                val page = remote.fetchMoviePage(
                    listType = listType,
                    page = FIRST_PAGE,
                    language = language,
                )
                database.withTransaction {
                    upsertMerged(page.results, language)
                    replaceList(listType, page, FIRST_PAGE)
                }
            }
        }
    }

    override suspend fun refreshAllMovies(language: MovieContentLanguage) {
        translateMovieFailures {
            listLocks.getValue(MovieListType.POPULAR).withLock {
                listLocks.getValue(MovieListType.TOP_RATED).withLock {
                    val popular = remote.fetchMoviePage(
                        listType = MovieListType.POPULAR,
                        page = FIRST_PAGE,
                        language = language,
                    )
                    val topRated = remote.fetchMoviePage(
                        listType = MovieListType.TOP_RATED,
                        page = FIRST_PAGE,
                        language = language,
                    )
                    languageGenerationLock.withLock {
                        database.withTransaction {
                            upsertMerged(popular.results + topRated.results, language)
                            replaceList(MovieListType.POPULAR, popular, FIRST_PAGE)
                            replaceList(MovieListType.TOP_RATED, topRated, FIRST_PAGE)
                        }
                        languageGeneration += 1
                    }
                }
            }
        }
    }

    override suspend fun loadMoreMovies(
        listType: MovieListType,
        language: MovieContentLanguage,
    ) {
        translateMovieFailures {
            listLocks.getValue(listType).withLock {
                val pagination = paginationDao.get(listType) ?: return@withLock
                if (pagination.lastLoadedPage <= 0 || pagination.lastLoadedPage >= pagination.totalPages) {
                    return@withLock
                }
                val nextPage = pagination.lastLoadedPage + 1
                val page = remote.fetchMoviePage(
                    listType = listType,
                    page = nextPage,
                    language = language,
                )
                database.withTransaction {
                    upsertMerged(page.results, language)
                    appendNewMemberships(listType, page.results, nextPage)
                    paginationDao.upsert(
                        pagination.copy(
                            lastLoadedPage = nextPage,
                            totalPages = page.totalPages,
                            totalResults = page.totalResults,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
    }

    override suspend fun refreshMovieDetail(
        movieId: Int,
        language: MovieContentLanguage,
    ) {
        translateMovieFailures {
            withDetailLock(movieId) {
                val capturedGeneration = languageGenerationLock.withLock { languageGeneration }
                val details = remote.fetchMovieDetails(movieId, language)
                languageGenerationLock.withLock {
                    if (capturedGeneration != languageGeneration) {
                        return@withLock
                    }
                    database.withTransaction {
                        val existing = movieDao.getById(movieId)
                        movieDao.upsertAll(listOf(details.toEntity(language, existing)))
                    }
                }
            }
        }
    }

    private suspend fun <T> withDetailLock(movieId: Int, block: suspend () -> T): T {
        val lock = detailLocks.compute(movieId) { _, current ->
            (current ?: DetailLock()).also { it.users += 1 }
        } ?: error("Detail lock was not created.")
        try {
            return lock.mutex.withLock { block() }
        } finally {
            detailLocks.compute(movieId) { _, current ->
                if (current !== lock) {
                    current
                } else {
                    current.users -= 1
                    if (current.users == 0) null else current
                }
            }
        }
    }

    private class DetailLock {
        val mutex = Mutex()
        var users = 0
    }

    private suspend fun upsertMerged(
        dtos: List<MovieDto>,
        language: MovieContentLanguage,
    ) {
        if (dtos.isEmpty()) {
            return
        }
        val existing = movieDao.getByIds(dtos.map { it.id }.distinct()).associateBy { it.id }
        val merged = LinkedHashMap<Int, MovieEntity>()
        for (dto in dtos) {
            val base = merged[dto.id] ?: existing[dto.id]
            merged[dto.id] = dto.toEntity(language, base)
        }
        movieDao.upsertAll(merged.values.toList())
    }

    private suspend fun replaceList(
        listType: MovieListType,
        page: MoviePageDto,
        requestedPage: Int,
    ) {
        val entries = page.results.distinctBy { it.id }.mapIndexed { index, dto ->
            MovieListEntity(
                movieId = dto.id,
                listType = listType,
                sortOrder = index,
                sourcePage = requestedPage,
            )
        }
        movieListDao.replaceForListType(listType, entries)
        paginationDao.upsert(
            MovieListPaginationEntity(
                listType = listType,
                lastLoadedPage = requestedPage,
                totalPages = page.totalPages,
                totalResults = page.totalResults,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun appendNewMemberships(
        listType: MovieListType,
        results: List<MovieDto>,
        sourcePage: Int,
    ) {
        val alreadyListed = movieListDao.movieIds(listType).toSet()
        val additions = results.distinctBy { it.id }.filter { it.id !in alreadyListed }
        if (additions.isEmpty()) {
            return
        }
        val firstSortOrder = movieListDao.maxSortOrder(listType) + 1
        movieListDao.append(
            additions.mapIndexed { index, dto ->
                MovieListEntity(
                    movieId = dto.id,
                    listType = listType,
                    sortOrder = firstSortOrder + index,
                    sourcePage = sourcePage,
                )
            },
        )
    }

    private fun <T> Flow<T>.translateFlowFailures(): Flow<T> {
        return catch { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
            throw throwable.toMovieException()
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
