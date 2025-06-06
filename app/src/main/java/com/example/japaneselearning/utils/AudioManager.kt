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
    
    fun playAudio(audioPath: String) {
        try {
            stopAudio() // Stop any currently playing audio
            
            val file = File(audioPath)
            if (!file.exists()) {
                Log.e(TAG, "Audio file does not exist: $audioPath")
                return
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()
                setOnCompletionListener {
                    Log.d(TAG, "Audio playback completed")
                }
            }
            Log.d(TAG, "Playing audio: $audioPath")
        } catch (e: IOException) {
            Log.e(TAG, "Error playing audio: ${e.message}")
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
    
    fun speakJapanese(text: String) {
        textToSpeech?.let { tts ->
            if (tts.isSpeaking) {
                tts.stop()
            }
            val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error speaking text: $text")
            } else {
                Log.d(TAG, "Speaking: $text")
            }
        } ?: Log.e(TAG, "TTS not initialized")
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