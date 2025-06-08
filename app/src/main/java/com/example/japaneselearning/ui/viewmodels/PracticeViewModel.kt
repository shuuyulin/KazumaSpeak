package com.example.japaneselearning.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.japaneselearning.data.database.AppDatabase
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.data.entities.Recording
import com.example.japaneselearning.data.repository.SentenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import java.io.File

data class PracticeSettings(
    val showJapanese: Boolean = true,
    val showRomaji: Boolean = false,
    val showAudio: Boolean = true,
    val autoPlay: Boolean = false,
    val reverseMode: Boolean = false
)

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SentenceRepository
    val allRecordings: LiveData<List<Recording>>
    
    private val _currentSentence = MutableLiveData<Sentence?>()
    val currentSentence: LiveData<Sentence?> = _currentSentence
    
    private val _practiceSettings = MutableLiveData(PracticeSettings())
    val practiceSettings: LiveData<PracticeSettings> = _practiceSettings
    
    private val _isCardFlipped = MutableLiveData(false)
    val isCardFlipped: LiveData<Boolean> = _isCardFlipped
    
    private var sentences: List<Sentence> = emptyList()
    private var currentIndex = 0
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = SentenceRepository(database)
        allRecordings = repository.getAllRecordings()
    }
    
    fun setSentences(sentenceList: List<Sentence>) {
        sentences = sentenceList
        currentIndex = 0
        if (sentences.isNotEmpty()) {
            _currentSentence.value = sentences[0]
        }
        _isCardFlipped.value = false
    }
    
    fun nextSentence() {
        if (sentences.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % sentences.size
            _currentSentence.value = sentences[currentIndex]
            _isCardFlipped.value = false
        }
    }
    
    fun flipCard() {
        _isCardFlipped.value = !(_isCardFlipped.value ?: false)
    }
    
    fun updateSettings(settings: PracticeSettings) {
        _practiceSettings.value = settings
    }
    
    fun saveRecording(sentenceId: Long, audioPath: String, similarityScore: Float) {
        viewModelScope.launch {
            try {
                val recording = Recording(
                    sentenceId = sentenceId,
                    audioPath = audioPath,
                    similarityScore = similarityScore,
                    createdAt = System.currentTimeMillis()
                )
                Log.d("PracticeViewModel", "Saving recording: $recording")
                repository.insertRecording(recording)
            } catch (e: Exception) {
                Log.e("PracticeViewModel", "Error saving recording: ${e.message}")
            }
        }
    }
    
    // Add this method if not already present
    suspend fun deleteRecording(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            // First delete the file
            val file = File(recording.audioPath)
            if (file.exists()) {
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.e("PracticeViewModel", "Error deleting audio file: ${e.message}")
                }
            }
            
            // Then delete from database
            val recordingDao = AppDatabase.getDatabase(getApplication()).recordingDao()
            recordingDao.deleteRecording(recording)
        }
    }
}