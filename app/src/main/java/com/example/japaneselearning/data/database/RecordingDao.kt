package com.example.japaneselearning.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.japaneselearning.data.entities.Recording

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): LiveData<List<Recording>>
    
    @Query("SELECT * FROM recordings WHERE sentenceId = :sentenceId")
    fun getRecordingsForSentence(sentenceId: Long): LiveData<List<Recording>>
    
    @Insert
    suspend fun insertRecording(recording: Recording): Long
    
    @Delete
    suspend fun deleteRecording(recording: Recording)
}