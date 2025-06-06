package com.example.japaneselearning.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.japaneselearning.data.entities.Sentence

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences ORDER BY createdAt DESC")
    fun getAllSentences(): LiveData<List<Sentence>>
    
    @Query("SELECT * FROM sentences WHERE id = :id")
    suspend fun getSentenceById(id: Long): Sentence?
    
    @Insert
    suspend fun insertSentence(sentence: Sentence): Long
    
    @Update
    suspend fun updateSentence(sentence: Sentence)
    
    @Delete
    suspend fun deleteSentence(sentence: Sentence)
    
    @Query("SELECT * FROM sentences WHERE category = :category")
    fun getSentencesByCategory(category: String): LiveData<List<Sentence>>
}