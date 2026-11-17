package dev.alexrincon.movieclient.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE movies ADD COLUMN title TEXT NOT NULL DEFAULT ''")
    }
}
