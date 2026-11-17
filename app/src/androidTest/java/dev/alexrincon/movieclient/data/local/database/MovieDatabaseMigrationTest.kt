package dev.alexrincon.movieclient.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private companion object {
        const val TEST_DB = "movie-migration-test"
    }
}
