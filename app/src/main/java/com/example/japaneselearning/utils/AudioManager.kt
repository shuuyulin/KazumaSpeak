package com.example.japaneselearning.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.*

class AudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var textToSpeech: TextToSpeech? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null
    
    companion object {
        private const val TAG = "AudioManager"
    }
    
    init {
        initTextToSpeech()
    }
    
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.JAPANESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Japanese language not supported for TTS")
                } else {
                    Log.d(TAG, "TTS initialized successfully")
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }
    
    fun playAudio(audioPath: String): Boolean {
        return try {
            stopAudio() // Stop any currently playing audio
            
            // Check if file exists
            val file = File(audioPath)
            if (!file.exists()) {
                Log.e(TAG, "Audio file does not exist: $audioPath")
                return false
            }
            
            // Check if file is readable and has content
            if (!file.canRead() || file.length() == 0L) {
                Log.e(TAG, "Audio file is not readable or empty: $audioPath")
                return false
            }
            
            Log.d(TAG, "Attempting to play audio: $audioPath")
            
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(audioPath)
                    
                    // Use prepareAsync instead of prepare to avoid blocking UI
                    setOnPreparedListener { player ->
                        Log.d(TAG, "MediaPlayer prepared, starting playback")
                        player.start()
                    }
                    
                    setOnCompletionListener { player ->
                        Log.d(TAG, "Audio playback completed")
                        player.release()
                        mediaPlayer = null
                    }
                    
                    setOnErrorListener { player, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        player.release()
                        mediaPlayer = null
                        false // Return false to trigger onCompletion
                    }
                    
                    prepareAsync() // Use async prepare
                    
                } catch (e: IOException) {
                    Log.e(TAG, "IOException setting data source: ${e.message}")
                    release()
                    mediaPlayer = null
                    return false
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException in MediaPlayer: ${e.message}")
                    release()
                    mediaPlayer = null
                    return false
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error playing audio: ${e.message}", e)
            stopAudio()
            false
        }
    }
    
    fun stopAudio() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio: ${e.message}")
            }
        }
        mediaPlayer = null
    }
    
    fun speakJapanese(text: String): Boolean {
        return try {
            textToSpeech?.let { tts ->
                if (tts.isSpeaking) {
                    tts.stop()
                }
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "Error speaking text: $text")
                    false
                } else {
                    Log.d(TAG, "Speaking: $text")
                    true
                }
            } ?: run {
                Log.e(TAG, "TTS not initialized")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in speakJapanese: ${e.message}", e)
            false
        }
    }
    
    fun startRecording(): File? {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return null
        }
        
        return try {
            currentRecordingFile = createAudioFile("recording")
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(currentRecordingFile!!.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Log.d(TAG, "Recording started: ${currentRecordingFile!!.absolutePath}")
            currentRecordingFile
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            null
        }
    }
    
    fun stopRecording(): File? {
        return if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                Log.d(TAG, "Recording stopped: ${currentRecordingFile?.absolutePath}")
                currentRecordingFile
            } catch (e: RuntimeException) {
                Log.e(TAG, "Error stopping recording: ${e.message}")
                null
            }
        } else {
            Log.w(TAG, "Not currently recording")
            null
        }
    }
    
    fun generateTTSAudio(text: String): File? {
        val audioFile = createAudioFile("tts_$text")
        
        textToSpeech?.let { tts ->
            val result = tts.synthesizeToFile(text, null, audioFile, null)
            return if (result == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS audio generated: ${audioFile.absolutePath}")
                audioFile
            } else {
                Log.e(TAG, "Failed to generate TTS audio")
                null
            }
        }
        return null
    }
    
    fun createAudioFile(prefix: String): File {
        val audioDir = File(context.getExternalFilesDir(null), "audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return File(audioDir, "${prefix}_${System.currentTimeMillis()}.3gp")
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun release() {
        stopAudio()
        stopRecording()
        textToSpeech?.shutdown()
    }
}