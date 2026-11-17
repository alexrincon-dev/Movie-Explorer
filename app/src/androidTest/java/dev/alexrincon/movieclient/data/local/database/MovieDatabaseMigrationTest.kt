package dev.alexrincon.movieclient.data.local.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.alexrincon.movieclient.data.local.entity.MovieEntity
import dev.alexrincon.movieclient.data.local.entity.MovieListPaginationEntity
import dev.alexrincon.movieclient.domain.model.MovieContentLanguage
import dev.alexrincon.movieclient.domain.model.MovieListType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MovieDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_addsLocalizedTitleAndKeepsTheCachedRow() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO movies (
                    id, originalTitle, overview, posterPath, backdropPath,
                    releaseDate, voteAverage, contentLanguage
                ) VALUES (
                    1, 'Other Mommy', 'English overview', 'poster.jpg', 'backdrop.jpg',
                    '2024-10-01', 8.0, 'EN_US'
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            MIGRATION_1_2,
        )
        val cursor = migrated.query(
            "SELECT originalTitle, title, overview, posterPath, contentLanguage FROM movies WHERE id = 1",
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("Other Mommy", cursor.getString(0))
        assertEquals("", cursor.getString(1))
        assertEquals("English overview", cursor.getString(2))
        assertEquals("poster.jpg", cursor.getString(3))
        assertEquals("EN_US", cursor.getString(4))
        cursor.close()
    }

    @Test
    fun migrate1To2_keepsListMembershipsAndPaginationAndOpensWithRoom() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO movies (
                    id, originalTitle, overview, posterPath, backdropPath,
                    releaseDate, voteAverage, contentLanguage
                ) VALUES
                    (1, 'Other Mommy', 'English overview', 'one.jpg', 'one-backdrop.jpg', '2024-10-01', 8.0, 'EN_US'),
                    (2, 'Inception', 'Sinopsis', NULL, NULL, NULL, 8.4, 'ES_MX'),
                    (3, 'Heat', 'Crime', 'three.jpg', NULL, '1995-12-15', 7.9, 'EN_US')
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO movie_lists (movieId, listType, sortOrder, sourcePage) VALUES
                    (2, 'POPULAR', 0, 1),
                    (1, 'POPULAR', 1, 1),
                    (2, 'TOP_RATED', 0, 1),
                    (3, 'TOP_RATED', 20, 2)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO movie_list_pagination (
                    listType, lastLoadedPage, totalPages, totalResults, updatedAt
                ) VALUES
                    ('POPULAR', 1, 500, 10000, 1700000000000),
                    ('TOP_RATED', 2, 450, 9000, 1700000000500)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        migrated.query(
            "SELECT movieId, listType, sortOrder, sourcePage FROM movie_lists ORDER BY listType, sortOrder",
        ).use { cursor ->
            val rows = buildList {
                while (cursor.moveToNext()) {
                    add(listOf(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), cursor.getInt(3)))
                }
            }
            assertEquals(
                listOf(
                    listOf(2, "POPULAR", 0, 1),
                    listOf(1, "POPULAR", 1, 1),
                    listOf(2, "TOP_RATED", 0, 1),
                    listOf(3, "TOP_RATED", 20, 2),
                ),
                rows,
            )
        }
        migrated.query("SELECT COUNT(*) FROM movies WHERE title = ''").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(3, cursor.getInt(0))
        }
        migrated.close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MovieDatabase::class.java,
            TEST_DB,
        ).addMigrations(MIGRATION_1_2).build()
        helper.closeWhenFinished(database)

        runBlocking {
            val popular = database.movieDao().observeByListType(MovieListType.POPULAR).first()
            val topRated = database.movieDao().observeByListType(MovieListType.TOP_RATED).first()
            assertEquals(listOf(2, 1), popular.map { it.id })
            assertEquals(listOf(2, 3), topRated.map { it.id })
            assertEquals(
                MovieListPaginationEntity(
                    listType = MovieListType.POPULAR,
                    lastLoadedPage = 1,
                    totalPages = 500,
                    totalResults = 10000,
                    updatedAt = 1_700_000_000_000,
                ),
                database.movieListPaginationDao().get(MovieListType.POPULAR),
            )
            assertEquals(
                MovieListPaginationEntity(
                    listType = MovieListType.TOP_RATED,
                    lastLoadedPage = 2,
                    totalPages = 450,
                    totalResults = 9000,
                    updatedAt = 1_700_000_000_500,
                ),
                database.movieListPaginationDao().get(MovieListType.TOP_RATED),
            )
            assertEquals(
                MovieEntity(
                    id = 2,
                    originalTitle = "Inception",
                    overview = "Sinopsis",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = null,
                    voteAverage = 8.4,
                    contentLanguage = MovieContentLanguage.ES_MX,
                    title = "",
                ),
                database.movieDao().getById(2),
            )
            assertEquals(
                MovieEntity(
                    id = 1,
                    originalTitle = "Other Mommy",
                    overview = "English overview",
                    posterPath = "one.jpg",
                    backdropPath = "one-backdrop.jpg",
                    releaseDate = "2024-10-01",
                    voteAverage = 8.0,
                    contentLanguage = MovieContentLanguage.EN_US,
                    title = "",
                ),
                database.movieDao().getById(1),
            )
        }
    }

    private companion object {
        const val TEST_DB = "movie-migration-test"
    }
}
