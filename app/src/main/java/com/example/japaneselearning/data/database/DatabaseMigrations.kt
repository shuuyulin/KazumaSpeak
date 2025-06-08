package com.example.japaneselearning.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 2 to 3:
 * - Added similarityScore field to recordings table
 */
val MIGRATION_2_3 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create a temporary table with the new schema
        database.execSQL(
            """
            CREATE TABLE recordings_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sentenceId INTEGER NOT NULL,
                audioPath TEXT NOT NULL,
                similarityScore REAL NOT NULL DEFAULT 75.0,
                createdAt INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
                FOREIGN KEY (sentenceId) REFERENCES sentences (id) ON DELETE CASCADE
            )
            """
        )

        // Copy data from the old table to the new one
        database.execSQL(
            """
            INSERT INTO recordings_new (id, sentenceId, audioPath, createdAt)
            SELECT id, sentenceId, audioPath, createdAt FROM recordings
            """
        )

        // Remove the old table
        database.execSQL("DROP TABLE recordings")

        // Rename the new table to the old table's name
        database.execSQL("ALTER TABLE recordings_new RENAME TO recordings")
    }
}