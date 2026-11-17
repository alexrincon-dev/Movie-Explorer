package dev.alexrincon.movieclient.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.alexrincon.movieclient.data.image.TmdbImageUrlBuilder
import dev.alexrincon.movieclient.data.local.database.MovieDatabase
import dev.alexrincon.movieclient.data.local.entity.MovieEntity
import dev.alexrincon.movieclient.data.local.entity.MovieListEntity
import dev.alexrincon.movieclient.data.local.entity.MovieListPaginationEntity
import dev.alexrincon.movieclient.data.remote.datasource.TmdbRemoteDataSource
import dev.alexrincon.movieclient.data.remote.dto.MovieDto
import dev.alexrincon.movieclient.data.remote.dto.MoviePageDto
import dev.alexrincon.movieclient.domain.error.MovieError
import dev.alexrincon.movieclient.domain.error.MovieException
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListSnapshot
import dev.alexrincon.movieclient.domain.model.MovieListType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class MovieRepositoryImplTest {
    private lateinit var database: MovieDatabase
    private lateinit var remote: FakeTmdbRemoteDataSource
    private lateinit var repository: MovieRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MovieDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        remote = FakeTmdbRemoteDataSource()
        repository = MovieRepositoryImpl(
            database = database,
            movieDao = database.movieDao(),
            movieListDao = database.movieListDao(),
            paginationDao = database.movieListPaginationDao(),
            remote = remote,
            imageUrls = TmdbImageUrlBuilder(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeMovies_returnsEmptyWhenNothingIsCached() = runBlocking {
        assertEquals(
            MovieListSnapshot.Empty,
            repository.observeMovies(MovieListType.POPULAR).first(),
        )
    }

    @Test
    fun refreshMovies_persistsRelativePathsMembershipAndPagination() = runBlocking {
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 3,
            movies = listOf(movie(1, "One", "/one.jpg"), movie(2, "Two", "/two.jpg")),
        )

        repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)

        val snapshot = repository.observeMovies(MovieListType.POPULAR).first()
        assertEquals(listOf(1, 2), snapshot.movies.map { it.id })
        assertEquals("https://image.tmdb.org/t/p/w500/one.jpg", snapshot.movies.first().posterPath)
        assertEquals(1, snapshot.lastLoadedPage)
        assertEquals(3, snapshot.totalPages)
        assertEquals("one.jpg", database.movieDao().getById(1)?.posterPath)
        assertEquals(MovieContentLanguage.EN_US, database.movieDao().getById(1)?.contentLanguage)
    }

    @Test
    fun refreshMovies_failureLeavesExistingData() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.failures[MovieListType.POPULAR] = IOException("offline")

        try {
            repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.ES_MX)
            fail()
        } catch (exception: MovieException) {
            assertEquals(MovieError.Network, exception.error)
        }

        val snapshot = repository.observeMovies(MovieListType.POPULAR).first()
        assertEquals(listOf(1), snapshot.movies.map { it.id })
        assertEquals("Cached", snapshot.movies.single().overview)
        assertEquals(1, snapshot.lastLoadedPage)
        assertEquals(MovieContentLanguage.EN_US, database.movieDao().getById(1)?.contentLanguage)
    }

    @Test
    fun refreshMovies_doesNotRemoveTheOtherList() = runBlocking {
        remote.pages[MovieListType.POPULAR to 1] = page(totalPages = 1, movies = listOf(movie(1, "Popular")))
        remote.pages[MovieListType.TOP_RATED to 1] = page(totalPages = 1, movies = listOf(movie(2, "Rated")))
        repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)
        repository.refreshMovies(MovieListType.TOP_RATED, MovieContentLanguage.EN_US)

        remote.pages[MovieListType.POPULAR to 1] = page(totalPages = 1, movies = listOf(movie(3, "Next")))
        repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)

        assertEquals(listOf(3), repository.observeMovies(MovieListType.POPULAR).first().movies.map { it.id })
        assertEquals(listOf(2), repository.observeMovies(MovieListType.TOP_RATED).first().movies.map { it.id })
    }

    @Test
    fun refreshMovies_keepsCachedPosterWhenListOmitsIt() = runBlocking {
        database.movieDao().upsertAll(
            listOf(
                MovieEntity(
                    id = 1,
                    originalTitle = "Title",
                    overview = "Detailed overview",
                    posterPath = "poster.jpg",
                    backdropPath = "backdrop.jpg",
                    releaseDate = "2020-01-01",
                    voteAverage = 8.0,
                    contentLanguage = MovieContentLanguage.EN_US,
                    title = "Localized",
                ),
            ),
        )
        database.movieListDao().append(
            listOf(MovieListEntity(1, MovieListType.POPULAR, sortOrder = 0, sourcePage = 1)),
        )
        database.movieListPaginationDao().upsert(
            MovieListPaginationEntity(MovieListType.POPULAR, 1, 1, 1, 1L),
        )
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(MovieDto(id = 1, originalTitle = "Title", overview = "", posterPath = null)),
        )

        repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)

        val stored = database.movieDao().getById(1)
        assertEquals("poster.jpg", stored?.posterPath)
        assertEquals("Detailed overview", stored?.overview)
        assertEquals("Localized", stored?.title)
        assertEquals("Title", stored?.originalTitle)
    }

    @Test
    fun loadMoreMovies_appendsInOrderAndSkipsDuplicates() = runBlocking {
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 2,
            movies = listOf(movie(1, "One"), movie(2, "Two")),
        )
        remote.pages[MovieListType.POPULAR to 2] = page(
            totalPages = 2,
            movies = listOf(movie(2, "Two again"), movie(3, "Three")),
        )
        repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)

        repository.loadMoreMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)

        val snapshot = repository.observeMovies(MovieListType.POPULAR).first()
        assertEquals(listOf(1, 2, 3), snapshot.movies.map { it.id })
        assertEquals(2, snapshot.lastLoadedPage)
        assertEquals(1, database.movieListDao().movieIds(MovieListType.POPULAR).count { it == 2 })
    }

    @Test
    fun loadMoreMovies_failureDoesNotAdvancePagination() = runBlocking {
        remote.pages[MovieListType.POPULAR to 1] = page(totalPages = 2, movies = listOf(movie(1, "One")))
        repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)
        remote.failures[MovieListType.POPULAR] = IOException("offline")

        try {
            repository.loadMoreMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)
            fail()
        } catch (exception: MovieException) {
            assertEquals(MovieError.Network, exception.error)
        }

        assertEquals(1, repository.observeMovies(MovieListType.POPULAR).first().lastLoadedPage)
        assertEquals(listOf(1), database.movieListDao().movieIds(MovieListType.POPULAR))
    }

    @Test
    fun loadMoreMovies_doesNothingBeforeTheFirstPage() = runBlocking {
        repository.loadMoreMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)

        assertTrue(remote.requestedPages.isEmpty())
        assertEquals(MovieListSnapshot.Empty, repository.observeMovies(MovieListType.POPULAR).first())
    }

    @Test
    fun refreshMovieDetail_preservesOmittedFieldsAndDoesNotChangeMembership() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.details[1] = MovieDto(id = 1, originalTitle = "Title", overview = "", posterPath = null)

        repository.refreshMovieDetail(1, MovieContentLanguage.EN_US)

        val stored = database.movieDao().getById(1)
        assertEquals("Cached", stored?.overview)
        assertEquals("poster.jpg", stored?.posterPath)
        assertEquals(listOf(1), database.movieListDao().movieIds(MovieListType.POPULAR))
        assertEquals(1, database.movieListPaginationDao().get(MovieListType.POPULAR)?.lastLoadedPage)
    }

    @Test
    fun refreshMovieDetail_languageChangeReplacesOverview() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.details[1] = MovieDto(id = 1, overview = "", posterPath = null)

        repository.refreshMovieDetail(1, MovieContentLanguage.ES_MX)

        val stored = database.movieDao().getById(1)
        assertEquals("", stored?.overview)
        assertEquals("", stored?.title)
        assertEquals("Title", stored?.originalTitle)
        assertEquals("poster.jpg", stored?.posterPath)
        assertEquals(MovieContentLanguage.ES_MX, stored?.contentLanguage)
    }

    @Test
    fun refreshMovieDetail_languageChangeReplacesLocalizedTitleAndOverview() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.details[1] = MovieDto(
            id = 1,
            title = "La otra madre",
            originalTitle = "Other Mommy",
            overview = "Sinopsis en español",
            posterPath = null,
        )

        repository.refreshMovieDetail(1, MovieContentLanguage.ES_MX)

        val stored = database.movieDao().getById(1)
        assertEquals("La otra madre", stored?.title)
        assertEquals("Other Mommy", stored?.originalTitle)
        assertEquals("Sinopsis en español", stored?.overview)
        assertEquals("poster.jpg", stored?.posterPath)
        assertEquals(MovieContentLanguage.ES_MX, stored?.contentLanguage)
        val domain = repository.observeMovie(1).first()
        assertEquals("La otra madre", domain?.title)
        assertEquals("Other Mommy", domain?.originalTitle)
        assertEquals("Sinopsis en español", domain?.overview)
    }

    @Test
    fun refreshMovies_blankLocalizedTitleKeepsTheCachedTitle() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(
                MovieDto(
                    id = 1,
                    title = " ",
                    originalTitle = "",
                    overview = "Still cached",
                    posterPath = null,
                ),
            ),
        )

        repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)

        val stored = database.movieDao().getById(1)
        assertEquals("Cached title", stored?.title)
        assertEquals("Title", stored?.originalTitle)
        assertEquals("Still cached", stored?.overview)
    }

    @Test
    fun refreshAllMovies_updatesBothListsInTheRequestedLanguage() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.pages[MovieListType.POPULAR to 1] = page(totalPages = 1, movies = listOf(movie(1, "Popular", overview = "Hola")))
        remote.pages[MovieListType.TOP_RATED to 1] = page(totalPages = 4, movies = listOf(movie(2, "Rated", overview = "También")))

        repository.refreshAllMovies(MovieContentLanguage.ES_MX)

        assertEquals("Hola", database.movieDao().getById(1)?.overview)
        assertEquals("Popular", database.movieDao().getById(1)?.title)
        assertEquals("Popular", database.movieDao().getById(1)?.originalTitle)
        assertEquals(MovieContentLanguage.ES_MX, database.movieDao().getById(1)?.contentLanguage)
        assertEquals(MovieContentLanguage.ES_MX, database.movieDao().getById(2)?.contentLanguage)
        assertEquals(listOf(1), database.movieListDao().movieIds(MovieListType.POPULAR))
        assertEquals(listOf(2), database.movieListDao().movieIds(MovieListType.TOP_RATED))
        assertEquals(4, database.movieListPaginationDao().get(MovieListType.TOP_RATED)?.totalPages)
    }

    @Test
    fun refreshAllMovies_failureKeepsCachedTextAndALaterSuccessReplacesIt() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(movie(1, "No debe guardarse", overview = "Nuevo", originalTitle = "Other Mommy")),
        )
        remote.failures[MovieListType.TOP_RATED] = IOException("offline")

        try {
            repository.refreshAllMovies(MovieContentLanguage.ES_MX)
            fail()
        } catch (exception: MovieException) {
            assertEquals(MovieError.Network, exception.error)
        }

        val unchanged = database.movieDao().getById(1)
        assertEquals("Cached title", unchanged?.title)
        assertEquals("Title", unchanged?.originalTitle)
        assertEquals("Cached", unchanged?.overview)
        assertEquals(MovieContentLanguage.EN_US, unchanged?.contentLanguage)
        assertEquals("poster.jpg", unchanged?.posterPath)

        remote.failures.remove(MovieListType.TOP_RATED)
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(
                movie(1, "La otra madre", overview = "Sinopsis", originalTitle = "Other Mommy"),
            ),
        )
        remote.pages[MovieListType.TOP_RATED to 1] = page(
            totalPages = 1,
            movies = listOf(movie(2, "Otra", overview = "También")),
        )

        repository.refreshAllMovies(MovieContentLanguage.ES_MX)

        val stored = database.movieDao().getById(1)
        assertEquals("La otra madre", stored?.title)
        assertEquals("Other Mommy", stored?.originalTitle)
        assertEquals("Sinopsis", stored?.overview)
        assertEquals(MovieContentLanguage.ES_MX, stored?.contentLanguage)
        assertEquals("poster.jpg", stored?.posterPath)
    }

    @Test
    fun failedLanguageRefreshDoesNotLetAnOlderRequestOverwriteNewerLanguageData() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.details[1] = MovieDto(
            id = 1,
            title = "Stale English title",
            originalTitle = "Title",
            overview = "Stale English",
            posterPath = "/poster.jpg",
            voteAverage = 8.0,
        )
        val detailStarted = CompletableDeferred<Unit>()
        val releaseDetail = CompletableDeferred<Unit>()
        remote.beforeDetail = {
            detailStarted.complete(Unit)
            releaseDetail.await()
        }
        val detail = async(Dispatchers.IO) {
            repository.refreshMovieDetail(1, MovieContentLanguage.EN_US)
        }
        detailStarted.await()

        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(movie(1, "No debe guardarse", overview = "Nuevo")),
        )
        remote.failures[MovieListType.TOP_RATED] = IOException("offline")
        try {
            repository.refreshAllMovies(MovieContentLanguage.ES_MX)
            fail()
        } catch (exception: MovieException) {
            assertEquals(MovieError.Network, exception.error)
        }
        assertEquals("Cached", database.movieDao().getById(1)?.overview)
        assertEquals("Cached title", database.movieDao().getById(1)?.title)

        remote.failures.remove(MovieListType.TOP_RATED)
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(
                movie(1, "La otra madre", overview = "Sinopsis", originalTitle = "Other Mommy"),
            ),
        )
        remote.pages[MovieListType.TOP_RATED to 1] = page(
            totalPages = 1,
            movies = listOf(movie(2, "Otra", overview = "También")),
        )
        repository.refreshAllMovies(MovieContentLanguage.ES_MX)

        releaseDetail.complete(Unit)
        detail.await()

        val stored = database.movieDao().getById(1)
        assertEquals("La otra madre", stored?.title)
        assertEquals("Other Mommy", stored?.originalTitle)
        assertEquals("Sinopsis", stored?.overview)
        assertEquals(MovieContentLanguage.ES_MX, stored?.contentLanguage)
    }

    @Test
    fun refreshAllMovies_failureLeavesBothListsUnchanged() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.pages[MovieListType.POPULAR to 1] = page(totalPages = 1, movies = listOf(movie(9, "New")))
        remote.failures[MovieListType.TOP_RATED] = IOException("offline")

        try {
            repository.refreshAllMovies(MovieContentLanguage.ES_MX)
            fail()
        } catch (exception: MovieException) {
            assertEquals(MovieError.Network, exception.error)
        }

        assertEquals(listOf(1), database.movieListDao().movieIds(MovieListType.POPULAR))
        assertTrue(database.movieListDao().movieIds(MovieListType.TOP_RATED).isEmpty())
        assertNull(database.movieDao().getById(9))
        assertEquals(MovieContentLanguage.EN_US, database.movieDao().getById(1)?.contentLanguage)
    }

    @Test
    fun refreshMovies_rethrowsCancellation() = runBlocking {
        val cancellation = CancellationException("stop")
        remote.failures[MovieListType.POPULAR] = cancellation

        try {
            repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)
            fail()
        } catch (thrown: CancellationException) {
            assertSame(cancellation, thrown)
        }
    }

    @Test
    fun refreshMovies_serializesSameListAndAllowsTheOtherList() = runBlocking {
        remote.pages[MovieListType.POPULAR to 1] = page(totalPages = 1, movies = listOf(movie(1, "One")))
        remote.pages[MovieListType.TOP_RATED to 1] = page(totalPages = 1, movies = listOf(movie(2, "Two")))
        val releasePopular = CompletableDeferred<Unit>()
        val popularEntries = AtomicInteger(0)
        remote.beforeFetch = { listType, _ ->
            if (listType == MovieListType.POPULAR && popularEntries.incrementAndGet() == 1) {
                releasePopular.await()
            }
        }

        val first = async(Dispatchers.IO) {
            repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)
        }
        withTimeout(5_000) {
            while (popularEntries.get() < 1) yield()
        }
        val topRated = async(Dispatchers.IO) {
            repository.refreshMovies(MovieListType.TOP_RATED, MovieContentLanguage.EN_US)
        }
        topRated.await()
        assertEquals(listOf(2), database.movieListDao().movieIds(MovieListType.TOP_RATED))
        assertEquals(1, popularEntries.get())

        // Runs on this thread until its first suspension, which is the Popular lock held by `first`.
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            repository.refreshMovies(MovieListType.POPULAR, MovieContentLanguage.EN_US)
        }
        assertTrue(second.isActive)
        assertEquals(1, popularEntries.get())
        releasePopular.complete(Unit)
        first.await()
        second.await()
        assertEquals(2, popularEntries.get())
    }

    @Test
    fun refreshMovieDetail_startedBeforeLanguageRefreshDoesNotOverwriteIt() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(movie(1, "Title", overview = "Hola")),
        )
        remote.pages[MovieListType.TOP_RATED to 1] = page(
            totalPages = 1,
            movies = listOf(movie(2, "Rated", overview = "También")),
        )
        remote.details[1] = MovieDto(
            id = 1,
            originalTitle = "Title",
            overview = "Stale English",
            posterPath = "/poster.jpg",
            voteAverage = 8.0,
        )
        val detailStarted = CompletableDeferred<Unit>()
        val releaseDetail = CompletableDeferred<Unit>()
        remote.beforeDetail = {
            detailStarted.complete(Unit)
            releaseDetail.await()
        }

        val detail = async(Dispatchers.IO) {
            repository.refreshMovieDetail(1, MovieContentLanguage.EN_US)
        }
        detailStarted.await()
        repository.refreshAllMovies(MovieContentLanguage.ES_MX)
        releaseDetail.complete(Unit)
        detail.await()

        val stored = database.movieDao().getById(1)
        assertEquals("Hola", stored?.overview)
        assertEquals(MovieContentLanguage.ES_MX, stored?.contentLanguage)
    }

    @Test
    fun refreshMovieDetail_sameMovieIsSerializedAndLaterInvocationWins() = runBlocking {
        val events = mutableListOf<String>()
        val firstInside = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        remote.detailQueue.add(MovieDto(id = 1, originalTitle = "Title", overview = "First"))
        remote.detailQueue.add(MovieDto(id = 1, originalTitle = "Title", overview = "Second"))
        remote.beforeDetail = {
            val callNumber = synchronized(events) {
                events += "enter"
                events.count { it == "enter" }
            }
            if (callNumber == 1) {
                firstInside.complete(Unit)
                releaseFirst.await()
                synchronized(events) { events += "first-fetch-finished" }
            } else {
                val firstFetchFinished = synchronized(events) {
                    events.contains("first-fetch-finished")
                }
                check(firstFetchFinished) {
                    "Second detail request overlapped the first."
                }
            }
        }

        val first = async(Dispatchers.IO) {
            repository.refreshMovieDetail(1, MovieContentLanguage.EN_US)
        }
        firstInside.await()
        val second = async(Dispatchers.IO) {
            repository.refreshMovieDetail(1, MovieContentLanguage.ES_MX)
        }
        releaseFirst.complete(Unit)
        first.await()
        second.await()

        val stored = database.movieDao().getById(1)
        assertEquals("Second", stored?.overview)
        assertEquals(MovieContentLanguage.ES_MX, stored?.contentLanguage)
    }

    @Test
    fun refreshMovieDetail_differentMoviesRemainIndependent() = runBlocking {
        val releaseFirst = CompletableDeferred<Unit>()
        val firstStarted = CompletableDeferred<Unit>()
        remote.details[1] = MovieDto(id = 1, originalTitle = "One", overview = "English")
        remote.details[2] = MovieDto(id = 2, originalTitle = "Two", overview = "Spanish")
        remote.beforeDetail = { movieId ->
            if (movieId == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
        }

        val first = async(Dispatchers.IO) {
            repository.refreshMovieDetail(1, MovieContentLanguage.EN_US)
        }
        firstStarted.await()
        withTimeout(5_000) {
            repository.refreshMovieDetail(2, MovieContentLanguage.ES_MX)
        }

        assertEquals(MovieContentLanguage.ES_MX, database.movieDao().getById(2)?.contentLanguage)
        releaseFirst.complete(Unit)
        first.await()
        assertEquals(MovieContentLanguage.EN_US, database.movieDao().getById(1)?.contentLanguage)
    }

    @Test
    fun refreshAllMovies_failureDoesNotInvalidateAnInFlightDetailWrite() = runBlocking {
        seedPopular(MovieContentLanguage.EN_US)
        remote.pages[MovieListType.POPULAR to 1] = page(
            totalPages = 1,
            movies = listOf(movie(9, "Should not be stored", overview = "Nuevo")),
        )
        remote.failures[MovieListType.TOP_RATED] = IOException("offline")
        remote.details[1] = MovieDto(
            id = 1,
            originalTitle = "Title",
            overview = "From detail",
            voteAverage = 8.0,
        )
        val detailStarted = CompletableDeferred<Unit>()
        val releaseDetail = CompletableDeferred<Unit>()
        remote.beforeDetail = {
            detailStarted.complete(Unit)
            releaseDetail.await()
        }

        val detail = async(Dispatchers.IO) {
            repository.refreshMovieDetail(1, MovieContentLanguage.EN_US)
        }
        detailStarted.await()
        try {
            repository.refreshAllMovies(MovieContentLanguage.ES_MX)
            fail()
        } catch (exception: MovieException) {
            assertEquals(MovieError.Network, exception.error)
        }
        releaseDetail.complete(Unit)
        detail.await()

        val stored = database.movieDao().getById(1)
        assertEquals("From detail", stored?.overview)
        assertEquals(MovieContentLanguage.EN_US, stored?.contentLanguage)
        assertNull(database.movieDao().getById(9))
    }

    private suspend fun seedPopular(language: MovieContentLanguage) {
        database.movieDao().upsertAll(
            listOf(
                MovieEntity(
                    id = 1,
                    originalTitle = "Title",
                    overview = "Cached",
                    posterPath = "poster.jpg",
                    backdropPath = "backdrop.jpg",
                    releaseDate = "2020-01-01",
                    voteAverage = 8.0,
                    contentLanguage = language,
                    title = "Cached title",
                ),
            ),
        )
        database.movieListDao().append(
            listOf(MovieListEntity(1, MovieListType.POPULAR, sortOrder = 0, sourcePage = 1)),
        )
        database.movieListPaginationDao().upsert(
            MovieListPaginationEntity(MovieListType.POPULAR, 1, 2, 1, 1L),
        )
    }

    private fun page(totalPages: Int, movies: List<MovieDto>): MoviePageDto {
        return MoviePageDto(
            page = 1,
            results = movies,
            totalPages = totalPages,
            totalResults = movies.size,
        )
    }

    private fun movie(
        id: Int,
        title: String,
        posterPath: String? = "/poster.jpg",
        overview: String = title,
        originalTitle: String = title,
    ): MovieDto {
        return MovieDto(
            id = id,
            title = title,
            originalTitle = originalTitle,
            overview = overview,
            posterPath = posterPath,
            voteAverage = 7.0,
        )
    }

    private class FakeTmdbRemoteDataSource : TmdbRemoteDataSource {
        val pages = mutableMapOf<Pair<MovieListType, Int>, MoviePageDto>()
        val details = mutableMapOf<Int, MovieDto>()
        val detailQueue = ArrayDeque<MovieDto>()
        val failures = mutableMapOf<MovieListType, Throwable>()
        val requestedPages = mutableListOf<Pair<MovieListType, Int>>()
        var beforeFetch: suspend (MovieListType, Int) -> Unit = { _, _ -> }
        var beforeDetail: suspend (Int) -> Unit = {}

        override suspend fun fetchMoviePage(
            listType: MovieListType,
            page: Int,
            language: MovieContentLanguage,
        ): MoviePageDto {
            requestedPages += listType to page
            beforeFetch(listType, page)
            failures[listType]?.let { throw it }
            return pages[listType to page] ?: error("Missing $listType page $page")
        }

        override suspend fun fetchMovieDetails(
            movieId: Int,
            language: MovieContentLanguage,
        ): MovieDto {
            beforeDetail(movieId)
            if (detailQueue.isNotEmpty()) {
                return detailQueue.removeFirst()
            }
            return details[movieId] ?: error("Missing details $movieId")
        }
    }
}
