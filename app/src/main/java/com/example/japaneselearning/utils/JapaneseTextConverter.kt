package com.example.japaneselearning.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.japaneselearning.api.ApiClient
import com.example.japaneselearning.api.JapaneseTextApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JapaneseTextConverter(context: Context) {
    private val apiService: JapaneseTextApiService = ApiClient.createApiService(context.cacheDir)
    private val prefs: SharedPreferences = context.getSharedPreferences("japanese_cache", Context.MODE_PRIVATE)
    private val kanjiToKanaConverter = KanjiToKanaConverter()
    
    companion object {
        private const val TAG = "JapaneseTextConverter"
        private const val CACHE_EXPIRY = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    /**
     * Convert Japanese with kanji to kana-only using HashMap
     */
    fun convertKanjiToKana(japaneseWithKanji: String): ConversionResult {
        if (japaneseWithKanji.isBlank()) {
            return ConversionResult.Success("")
        }
        
        return try {
            val kanaResult = kanjiToKanaConverter.convertToKana(japaneseWithKanji)
            val coverage = kanjiToKanaConverter.getConversionCoverage(japaneseWithKanji)
            
            if (coverage < 100f) {
                ConversionResult.PartialSuccess(
                    result = kanaResult,
                    message = "Some kanji could not be converted (${coverage.toInt()}% coverage)"
                )
            } else {
                ConversionResult.Success(kanaResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting kanji to kana", e)
            ConversionResult.Error("Failed to convert kanji: ${e.message}")
        }
    }
    
    /**
     * Convert kana to romaji using API
     */
    suspend fun convertKanaToRomaji(kanaText: String): ConversionResult {
        if (kanaText.isBlank()) {
            return ConversionResult.Success("")
        }
        
        val cacheKey = "romaji_${kanaText.hashCode()}"
        val cached = getCachedResult(cacheKey)
        if (cached != null) {
            return ConversionResult.Success(cached)
        }
        
        return try {
            withContext(Dispatchers.IO) {
                val response = apiService.convertToRomaji(kanaText)
                if (response.isSuccessful) {
                    val result = response.body()?.a ?: kanaText
                    cacheResult(cacheKey, result)
                    ConversionResult.Success(result)
                } else {
                    Log.e(TAG, "API Error: ${response.code()} - ${response.message()}")
                    ConversionResult.Error("API Error: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error converting to romaji", e)
            ConversionResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Full conversion workflow: Japanese with kanji → kana → romaji
     */
    suspend fun convertJapaneseToRomaji(japaneseWithKanji: String): ConversionResult {
        // Step 1: Convert kanji to kana
        val kanaResult = convertKanjiToKana(japaneseWithKanji)
        if (kanaResult !is ConversionResult.Success && kanaResult !is ConversionResult.PartialSuccess) {
            return kanaResult
        }
        
        val kanaText = when (kanaResult) {
            is ConversionResult.Success -> kanaResult.result
            is ConversionResult.PartialSuccess -> kanaResult.result
            else -> return ConversionResult.Error("Unexpected result type")
        }
        
        // Step 2: Convert kana to romaji
        return convertKanaToRomaji(kanaText)
    }
    
    // Cache management (existing methods)
    private fun getCachedResult(key: String): String? {
        val timestamp = prefs.getLong("${key}_time", 0)
        return if (System.currentTimeMillis() - timestamp < CACHE_EXPIRY) {
            prefs.getString(key, null)
        } else {
            null
        }
    }
    
    private fun cacheResult(key: String, result: String) {
        prefs.edit()
            .putString(key, result)
            .putLong("${key}_time", System.currentTimeMillis())
            .apply()
    }
    
    fun clearOldCache() {
        val allEntries = prefs.all
        val editor = prefs.edit()
        val currentTime = System.currentTimeMillis()
        
        allEntries.forEach { (key, _) ->
            if (key.endsWith("_time")) {
                val timestamp = prefs.getLong(key, 0)
                if (currentTime - timestamp > CACHE_EXPIRY) {
                    val dataKey = key.removeSuffix("_time")
                    editor.remove(key)
                    editor.remove(dataKey)
                }
            }
        }
        editor.apply()
    }
}
// Result classes for better error handling
sealed class ConversionResult {
    data class Success(val result: String) : ConversionResult()
    data class PartialSuccess(val result: String, val message: String) : ConversionResult()
    data class Error(val message: String) : ConversionResult()
}