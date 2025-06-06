package com.example.japaneselearning.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.data.entities.Recording

@Database(
    entities = [Sentence::class, Recording::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentenceDao(): SentenceDao
    abstract fun recordingDao(): RecordingDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "japanese_learning_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}