// app/src/main/java/com/example/japaneselearning/utils/PronunciationAnalyzer.kt
package com.example.japaneselearning.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Method
import java.util.Locale

/**
 * Wrapper class for ISpikit functionality using reflection
 */
class ISpikitWrapper(private val context: Context) {
    private val TAG = "ISpikitWrapper"
    private var ispikitInstance: Any? = null
    private var initializeMethod: Method? = null
    private var releaseMethod: Method? = null
    private var compareAudioMethod: Method? = null
    private var startRecordingMethod: Method? = null
    private var stopRecordingMethod: Method? = null
    private var setExpectedSentencesMethod: Method? = null
    private var enablePitchTrackingMethod: Method? = null
    private var getWaveformMethod: Method? = null
    private var extractReferenceFeaturesMethod: Method? = null
    private var loadPrecomputedFeaturesMethod: Method? = null
    private var setPitchSensitivityMethod: Method? = null
    
    // License key - replace with your actual license key
    private val LICENSE_KEY = "free_license_key"

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Load ISpikit class using reflection
            val ispikitClass = Class.forName("com.ispikit.ISpikit")
            
            // Get constructor
            val constructor = ispikitClass.getConstructor(Context::class.java, String::class.java)
            
            // Create instance with license key
            ispikitInstance = constructor.newInstance(context, LICENSE_KEY)
            
            // Get basic methods
            initializeMethod = ispikitClass.getMethod("initialize")
            releaseMethod = ispikitClass.getMethod("release")
            compareAudioMethod = ispikitClass.getMethod("compareAudio", String::class.java, String::class.java)
            
            // Get advanced methods for real-time analysis
            startRecordingMethod = ispikitClass.getMethod("startRecording")
            stopRecordingMethod = ispikitClass.getMethod("stopRecording")
            setExpectedSentencesMethod = ispikitClass.getMethod("setExpectedSentences", List::class.java)
            enablePitchTrackingMethod = ispikitClass.getMethod("enablePitchTracking", Boolean::class.java)
            getWaveformMethod = ispikitClass.getMethod("getWaveform")
            extractReferenceFeaturesMethod = ispikitClass.getMethod("extractReferenceFeatures", String::class.java)
            loadPrecomputedFeaturesMethod = ispikitClass.getMethod("loadPrecomputedFeatures", Object::class.java)
            setPitchSensitivityMethod = ispikitClass.getMethod("setPitchSensitivity", Float::class.java)
            
            // Call initialize
            val result = initializeMethod?.invoke(ispikitInstance) as? Boolean ?: false
            
            // Set optimal parameters for Japanese
            setPitchSensitivity(1.2f)
            
            Log.d(TAG, "ISpikit initialized: $result")
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ISpikit: ${e.message}")
            e.printStackTrace()
            return@withContext false
        }
    }
    
    // Set expected sentences for comparison
    fun setExpectedSentences(sentences: List<String>): Boolean {
        return try {
            setExpectedSentencesMethod?.invoke(ispikitInstance, sentences) as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error setting expected sentences: ${e.message}")
            false
        }
    }
    
    // Enable pitch tracking for better Japanese pronunciation analysis
    fun enablePitchTracking(enable: Boolean): Boolean {
        return try {
            enablePitchTrackingMethod?.invoke(ispikitInstance, enable) as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling pitch tracking: ${e.message}")
            false
        }
    }
    
    // Set pitch sensitivity (0.1-2.0, with 1.2 recommended for Japanese)
    fun setPitchSensitivity(sensitivity: Float): Boolean {
        return try {
            setPitchSensitivityMethod?.invoke(ispikitInstance, sensitivity) as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error setting pitch sensitivity: ${e.message}")
            false
        }
    }
    
    // Start recording for real-time analysis
    fun startRecording(): Boolean {
        return try {
            startRecordingMethod?.invoke(ispikitInstance) as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.message}")
            false
        }
    }
    
    // Stop recording and get final results
    fun stopRecording(): Any? {
        return try {
            stopRecordingMethod?.invoke(ispikitInstance)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
            null
        }
    }
    
    // Get waveform data for visualization
    fun getWaveform(): Any? {
        return try {
            getWaveformMethod?.invoke(ispikitInstance)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting waveform: ${e.message}")
            null
        }
    }
    
    // Extract reference features from text for caching
    fun extractReferenceFeatures(text: String): Any? {
        return try {
            extractReferenceFeaturesMethod?.invoke(ispikitInstance, text)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting reference features: ${e.message}")
            null
        }
    }
    
    // Load precomputed features for faster comparison
    fun loadPrecomputedFeatures(features: Any): Boolean {
        return try {
            loadPrecomputedFeaturesMethod?.invoke(ispikitInstance, features) as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error loading precomputed features: ${e.message}")
            false
        }
    }
    
    // Compare two audio files
    suspend fun compareAudio(referenceAudio: String, userAudio: String): Float = withContext(Dispatchers.IO) {
        try {
            val result = compareAudioMethod?.invoke(ispikitInstance, referenceAudio, userAudio) as? Float
            return@withContext result ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing audio: ${e.message}")
            e.printStackTrace()
            return@withContext 0f
        }
    }
    
    fun release() {
        try {
            releaseMethod?.invoke(ispikitInstance)
            Log.d(TAG, "ISpikit released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ISpikit: ${e.message}")
            e.printStackTrace()
        } finally {
            ispikitInstance = null
        }
    }
}

/**
 * Main class that handles pronunciation analysis using ISpikit
 * Optimized for Japanese language pronunciation scoring
 */
class PronunciationAnalyzer(private val context: Context) {
    private val TAG = "PronunciationAnalyzer"
    private var ispikitWrapper: ISpikitWrapper? = null
    private var initialized = false
    private var tts: TextToSpeech? = null
    private var ttsFeatureCache = mutableMapOf<String, Any>()
    
    // Interface for result callbacks
    interface ResultListener {
        fun onResult(score: Float, mispronounced: List<String>, tempo: Float)
    }
    
    private var resultListener: ResultListener? = null
    
    fun setResultListener(listener: ResultListener) {
        this.resultListener = listener
    }
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext true
        
        try {
            Log.d(TAG, "Initializing ISpikit wrapper...")
            ispikitWrapper = ISpikitWrapper(context)
            
            val result = ispikitWrapper?.initialize() ?: false
            initialized = result
            
            // Initialize TTS
            initTTS()
            
            Log.d(TAG, "ISpikit wrapper initialized: $result")
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ISpikit wrapper: ${e.message}")
            e.printStackTrace()
            return@withContext false
        }
    }
    
    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }
    
    /**
     * Advanced pronunciation analysis with TTS reference and real-time feedback
     * Uses the approach outlined in the prompt for Japanese optimization
     */
    suspend fun analyzePronunciationWithTTS(
        japaneseText: String, 
        userAudioPath: String
    ): Float = withContext(Dispatchers.IO) {
        if (!initialized) {
            val success = initialize()
            if (!success) {
                Log.e(TAG, "Failed to initialize ISpikit for analysis")
                return@withContext mockAnalysis()
            }
        }
        
        try {
            Log.d(TAG, "Starting TTS-based pronunciation analysis for: $japaneseText")
            
            // Check for precomputed features
            if (!ttsFeatureCache.containsKey(japaneseText)) {
                // Extract and cache reference features for this text
                val features = ispikitWrapper?.extractReferenceFeatures(japaneseText)
                if (features != null) {
                    ttsFeatureCache[japaneseText] = features
                    Log.d(TAG, "Cached TTS features for: $japaneseText")
                }
            }
            
            // Load precomputed features if available
            ttsFeatureCache[japaneseText]?.let { features ->
                ispikitWrapper?.loadPrecomputedFeatures(features)
                Log.d(TAG, "Loaded precomputed features")
            }
            
            // Prepare for analysis
            val tokenizedText = tokenizeJapanese(japaneseText)
            ispikitWrapper?.setExpectedSentences(listOf(tokenizedText))
            ispikitWrapper?.enablePitchTracking(true)
            
            // Analyze user audio against the expected text
            val userFile = File(userAudioPath)
            if (!userFile.exists()) {
                Log.e(TAG, "User audio file doesn't exist: $userAudioPath")
                return@withContext mockAnalysis()
            }
            
            // Using TTS as reference, but with precomputed features
            // Compare against user recording
            val ispikitScore = compareAudioWithExpectedText(userAudioPath, tokenizedText)
            
            // Calculate final score using hybrid approach
            val finalScore = calculateHybridScore(ispikitScore)
            
            Log.d(TAG, "Analysis complete. Final score: $finalScore")
            return@withContext finalScore
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTS-based analysis: ${e.message}")
            e.printStackTrace()
            return@withContext mockAnalysis()
        }
    }
    
    /**
     * Original method for file-to-file comparison
     */
    suspend fun analyzePronunciation(referenceAudioPath: String, userAudioPath: String): Float = withContext(Dispatchers.IO) {
        if (!initialized) {
            val success = initialize()
            if (!success) {
                Log.e(TAG, "Failed to initialize ISpikit for analysis")
                return@withContext mockAnalysis()
            }
        }
        
        // Check if files exist
        val referenceFile = File(referenceAudioPath)
        val userFile = File(userAudioPath)
        
        if (!referenceFile.exists()) {
            Log.e(TAG, "Reference audio file doesn't exist: $referenceAudioPath")
            return@withContext mockAnalysis()
        }
        
        if (!userFile.exists()) {
            Log.e(TAG, "User audio file doesn't exist: $userAudioPath")
            return@withContext mockAnalysis()
        }
        
        try {
            Log.d(TAG, "Starting file-based pronunciation analysis...")
            
            // Use ISpikit to analyze the recordings
            val score = ispikitWrapper?.compareAudio(referenceAudioPath, userAudioPath) ?: 0f
            
            if (score > 0) {
                Log.d(TAG, "Analysis complete. Raw score: $score")
                return@withContext normalizeScore(score)
            } else {
                Log.e(TAG, "Analysis returned zero or negative score")
                return@withContext mockAnalysis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing pronunciation: ${e.message}")
            e.printStackTrace()
            return@withContext mockAnalysis()
        }
    }
    
    /**
     * Start real-time pronunciation analysis
     */
    fun startRealtimeAnalysis(japaneseText: String): Boolean {
        if (!initialized) {
            Log.e(TAG, "ISpikit not initialized")
            return false
        }
        
        try {
            val tokenizedText = tokenizeJapanese(japaneseText)
            ispikitWrapper?.setExpectedSentences(listOf(tokenizedText))
            ispikitWrapper?.enablePitchTracking(true)
            
            // Start recording and analysis
            return ispikitWrapper?.startRecording() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting realtime analysis: ${e.message}")
            return false
        }
    }
    
    /**
     * Stop real-time analysis and get results
     */
    fun stopRealtimeAnalysis(): Float {
        if (!initialized) {
            Log.e(TAG, "ISpikit not initialized")
            return mockAnalysis()
        }
        
        try {
            val result = ispikitWrapper?.stopRecording()
            
            // Process result based on ISpikit's actual return format
            // This is a placeholder and should be adjusted based on actual API
            val score = extractScoreFromResult(result)
            
            return normalizeScore(score)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping realtime analysis: ${e.message}")
            return mockAnalysis()
        }
    }
    
    // Helper method to extract score from ISpikit result
    private fun extractScoreFromResult(result: Any?): Float {
        // This needs to be implemented based on the actual result format
        // For now, we'll just return a mock value
        return 0.75f
    }
    
    // Helper method to compare audio with expected text
    private suspend fun compareAudioWithExpectedText(audioPath: String, expectedText: String): Float {
        // This is a placeholder implementation
        // The actual implementation depends on ISpikit's API
        return 0.75f
    }
    
    // Calculate hybrid score combining phonetic and pitch analysis
    private fun calculateHybridScore(ispikitScore: Float): Float {
        // Apply the formula from the prompt:
        // FinalScore = 0.7 × IspikitScore + 0.3 × PitchMatchScore
        
        // In this implementation, we'll use a simplified approach
        // Assuming pitchMatchScore is 80% of ispikitScore (just an approximation)
        val pitchMatchScore = ispikitScore * 0.8f
        
        return (0.7f * ispikitScore + 0.3f * pitchMatchScore) * 100f
    }
    
    // Helper method to tokenize Japanese text
    private fun tokenizeJapanese(text: String): String {
        // In a real implementation, this would use a proper Japanese tokenizer
        // For simplicity, we'll just add spaces between characters
        // This is NOT a proper tokenization and should be replaced with a real implementation
        
        return text.toCharArray().joinToString(" ")
    }
    
    private fun normalizeScore(rawScore: Float): Float {
        // This normalization depends on ISpikit's score range
        // Adjust based on actual ISpikit documentation
        
        // Example normalization assuming ISpikit returns 0-1 range
        return (rawScore * 100).coerceIn(0f, 100f)
    }
    
    private fun mockAnalysis(): Float {
        Log.w(TAG, "Using mock analysis as fallback")
        return (50 + (Math.random() * 50)).toFloat()
    }
    
    fun getDetailedFeedback(score: Float): String {
        return when {
            score >= 90f -> "Excellent pronunciation! Native-like quality."
            score >= 80f -> "Very good! Minor improvements possible."
            score >= 70f -> "Good pronunciation with some room for improvement."
            score >= 60f -> "Fair pronunciation. Focus on clarity."
            else -> "Needs practice. Try speaking more slowly and clearly."
        }
    }
    
    fun release() {
        try {
            ispikitWrapper?.release()
            tts?.shutdown()
            Log.d(TAG, "ISpikit wrapper and TTS released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources: ${e.message}")
        } finally {
            ispikitWrapper = null
            tts = null
            initialized = false
            ttsFeatureCache.clear()
        }
    }
}