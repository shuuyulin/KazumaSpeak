package com.example.japaneselearning.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.japaneselearning.data.database.AppDatabase
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.data.repository.SentenceRepository
import kotlinx.coroutines.launch

class SentenceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SentenceRepository
    val allSentences: LiveData<List<Sentence>>
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = SentenceRepository(database)
        allSentences = repository.getAllSentences()
    }
    
    fun insertSentence(sentence: Sentence) = viewModelScope.launch {
        repository.insertSentence(sentence)
    }
    
    fun updateSentence(sentence: Sentence) = viewModelScope.launch {
        repository.updateSentence(sentence)
    }
    
    fun deleteSentence(sentence: Sentence) = viewModelScope.launch {
        repository.deleteSentence(sentence)
    }
}