package com.example.japaneselearning.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
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
    private var ttsInitialized = false
    private var ttsQueue = mutableListOf<String>()
    private var currentRecordingFile: File? = null
    
    private val TAG = "AudioManager"

    init {
        initTTS()
    }

    private fun initTTS() {
        Log.d(TAG, "Initializing TTS...")
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.JAPANESE)
                ttsInitialized = (result != TextToSpeech.LANG_MISSING_DATA && 
                                 result != TextToSpeech.LANG_NOT_SUPPORTED)
                
                if (ttsInitialized) {
                    Log.d(TAG, "TTS initialized successfully for Japanese")
                    // Process any queued TTS requests
                    if (ttsQueue.isNotEmpty()) {
                        ttsQueue.forEach { text ->
                            textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, "tts_japanese")
                        }
                        ttsQueue.clear()
                    }
                } else {
                    Log.e(TAG, "Japanese language not supported by TTS")
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    fun speakJapanese(text: String): Boolean {
        // Always properly release the MediaPlayer before using TTS
        releaseMediaPlayer()
        
        if (ttsInitialized) {
            Log.d(TAG, "Speaking with TTS: $text")
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_japanese")
            return true
        } else {
            Log.d(TAG, "TTS not ready, queueing: $text")
            // Queue the text to be spoken once TTS is initialized
            ttsQueue.add(text)
            // If it's been more than 5 seconds, try reinitializing TTS
            Handler(Looper.getMainLooper()).postDelayed({
                if (!ttsInitialized) {
                    Log.d(TAG, "Re-initializing TTS after timeout")
                    initTTS()
                }
            }, 5000)
            return false
        }
    }

    fun playAudio(filePath: String): Boolean {
        try {
            // Stop TTS if it's speaking
            if (textToSpeech?.isSpeaking == true) {
                textToSpeech?.stop()
            }
            
            // Release previous MediaPlayer instance if it exists
            releaseMediaPlayer()
            
            // Verify file exists
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Audio file does not exist: $filePath")
                return false
            }
            
            Log.d(TAG, "Playing audio file: $filePath")
            
            // Create new MediaPlayer instance
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener { 
                    Log.d(TAG, "MediaPlayer prepared, starting playback")
                    it.start() 
                }
                setOnCompletionListener { 
                    Log.d(TAG, "MediaPlayer playback completed")
                    releaseMediaPlayer() 
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    releaseMediaPlayer()
                    true
                }
                prepareAsync()
            }
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Error playing audio: ${e.message}")
            releaseMediaPlayer()
            return false
        }
    }

    fun startRecording(): File? {
        if (isRecording) {
            return null
        }

        try {
            // Create recording file
            val fileName = "recording_${System.currentTimeMillis()}.3gp"
            val dir = context.getExternalFilesDir(null)
            currentRecordingFile = File(dir, fileName)
            
            // Release any existing resources
            releaseMediaPlayer()
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(currentRecordingFile?.absolutePath)
                
                try {
                    Log.d(TAG, "Starting recording to ${currentRecordingFile?.absolutePath}")
                    prepare()
                    start()
                    isRecording = true
                    return currentRecordingFile
                } catch (e: IOException) {
                    Log.e(TAG, "Recording preparation failed: ${e.message}")
                    release()
                    mediaRecorder = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording setup failed: ${e.message}")
            releaseRecorder()
        }
        
        return null
    }

    fun startRecording(outputFile: File): Boolean {
        if (isRecording) {
            return false
        }

        try {
            // Release any existing resources
            releaseMediaPlayer()
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile.absolutePath)
                
                try {
                    Log.d(TAG, "Starting recording to ${outputFile.absolutePath}")
                    prepare()
                    start()
                    isRecording = true
                    currentRecordingFile = outputFile
                    return true
                } catch (e: IOException) {
                    Log.e(TAG, "Recording preparation failed: ${e.message}")
                    release()
                    mediaRecorder = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording setup failed: ${e.message}")
            releaseRecorder()
        }
        
        return false
    }

    fun stopRecording(): File? {
        if (!isRecording) {
            return null
        }

        try {
            Log.d(TAG, "Stopping recording")
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            // Return the recorded file
            return if (currentRecordingFile?.exists() == true) {
                Log.d(TAG, "Recording saved to ${currentRecordingFile?.absolutePath}")
                currentRecordingFile
            } else {
                Log.e(TAG, "Recording file not found: ${currentRecordingFile?.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
            releaseRecorder()
        }
        
        return null
    }
    
    fun releaseMediaPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
                Log.d(TAG, "MediaPlayer released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer: ${e.message}")
        }
        mediaPlayer = null
    }
    
    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
            Log.d(TAG, "MediaRecorder released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder: ${e.message}")
        }
        mediaRecorder = null
        isRecording = false
    }

    fun release() {
        releaseMediaPlayer()
        releaseRecorder()
        
        textToSpeech?.apply {
            stop()
            shutdown()
            Log.d(TAG, "TTS engine shutdown")
        }
        textToSpeech = null
        ttsInitialized = false
    }
    
    /**
     * Creates an empty audio file with specified prefix
     */
    fun createAudioFile(prefix: String): File {
        val fileName = "${prefix}_${System.currentTimeMillis()}.3gp"
        val dir = context.getExternalFilesDir(null)
        return File(dir, fileName)
    }
}