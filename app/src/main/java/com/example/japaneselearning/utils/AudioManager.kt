// app/src/main/java/com/example/japaneselearning/utils/AudioManager.kt
package com.example.japaneselearning.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import java.io.File
import java.io.IOException
import java.util.*

class AudioManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var textToSpeech: TextToSpeech? = null
    private var isRecording = false
    
    init {
        initTextToSpeech()
    }
    
    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.JAPANESE
            }
        }
    }
    
    fun playAudio(audioPath: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    fun speakJapanese(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun startRecording(outputPath: String): Boolean {
        return try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputPath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    
    fun stopRecording(): String? {
        return if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                "Recording saved"
            } catch (e: RuntimeException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
    
    fun createAudioFile(prefix: String): File {
        val audioDir = File(context.getExternalFilesDir(null), "audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return File(audioDir, "${prefix}_${System.currentTimeMillis()}.3gp")
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaRecorder?.release()
        textToSpeech?.shutdown()
    }
}