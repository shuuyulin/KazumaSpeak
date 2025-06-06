package com.example.japaneselearning.data.repository

import androidx.lifecycle.LiveData
import com.example.japaneselearning.data.database.AppDatabase
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.data.entities.Recording

class SentenceRepository(private val database: AppDatabase) {
    private val sentenceDao = database.sentenceDao()
    private val recordingDao = database.recordingDao()
    
    fun getAllSentences(): LiveData<List<Sentence>> = sentenceDao.getAllSentences()
    
    suspend fun insertSentence(sentence: Sentence): Long = sentenceDao.insertSentence(sentence)
    
    suspend fun updateSentence(sentence: Sentence) = sentenceDao.updateSentence(sentence)
    
    suspend fun deleteSentence(sentence: Sentence) = sentenceDao.deleteSentence(sentence)
    
    fun getAllRecordings(): LiveData<List<Recording>> = recordingDao.getAllRecordings()
    
    suspend fun insertRecording(recording: Recording): Long = recordingDao.insertRecording(recording)
    
    suspend fun getSentenceById(id: Long): Sentence? = sentenceDao.getSentenceById(id)
}