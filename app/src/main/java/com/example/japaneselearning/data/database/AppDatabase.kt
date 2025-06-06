package com.example.japaneselearning.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.data.entities.Recording

@Database(
    entities = [Sentence::class, Recording::class],
    version = 2, // Increment version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentenceDao(): SentenceDao
    abstract fun recordingDao(): RecordingDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add kana column to sentences table
                database.execSQL("ALTER TABLE sentences ADD COLUMN kana TEXT NOT NULL DEFAULT ''")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "japanese_learning_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}