package com.example.japaneselearning.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.japaneselearning.R
import com.example.japaneselearning.data.entities.Recording
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.data.repository.SentenceRepository
import com.example.japaneselearning.databinding.ItemRecordingBinding
import com.example.japaneselearning.utils.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RecordingAdapter(
    private val repository: SentenceRepository,
    private val audioManager: AudioManager
) : ListAdapter<Recording, RecordingAdapter.RecordingViewHolder>(RecordingDiffCallback()) {

    private val TAG = "RecordingAdapter"
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val sentenceCache = mutableMapOf<Long, Sentence?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val binding = ItemRecordingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordingViewHolder(
        private val binding: ItemRecordingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: Recording) {
            binding.apply {
                // Set score info first
                scoreText.text = "${recording.similarityScore.toInt()}%"

                // Set score indicator color based on score
                val (backgroundColor, indicatorColor, textColor) = when {
                    recording.similarityScore >= 90f -> Triple(
                        R.color.green_light,
                        R.color.green_primary,
                        R.color.green_primary
                    )
                    recording.similarityScore >= 70f -> Triple(
                        R.color.orange_light,
                        R.color.orange_primary,
                        R.color.orange_primary
                    )
                    else -> Triple(
                        R.color.red_light,
                        R.color.red_primary,
                        R.color.red_primary
                    )
                }

                scoreCard.setCardBackgroundColor(ContextCompat.getColor(root.context, backgroundColor))
                scoreIndicator.backgroundTintList = ContextCompat.getColorStateList(root.context, indicatorColor)
                scoreText.setTextColor(ContextCompat.getColor(root.context, textColor))

                // Format time
                val timeAgo = formatTimeAgo(recording.createdAt)
                timeText.text = timeAgo

                // Load sentence data for this recording
                loadSentenceData(recording.sentenceId)
                
                // Check if recording file exists
                val recordingFile = File(recording.audioPath)
                if (!recordingFile.exists()) {
                    Log.e(TAG, "Recording file doesn't exist: ${recording.audioPath}")
                    btnPlayRecording.isEnabled = false
                    btnPlayRecording.alpha = 0.5f
                } else {
                    btnPlayRecording.isEnabled = true
                    btnPlayRecording.alpha = 1.0f
                }
                
                // Set up audio playback
                btnPlayOriginal.setOnClickListener {
                    sentenceCache[recording.sentenceId]?.let { sentence ->
                        if (!sentence.audioPath.isNullOrEmpty()) {
                            val file = File(sentence.audioPath)
                            if (file.exists()) {
                                val success = audioManager.playAudio(sentence.audioPath)
                                if (!success) {
                                    Toast.makeText(root.context, "Failed to play original audio", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.d(TAG, "Original audio file doesn't exist, using TTS")
                                // Fallback to TTS
                                val textToSpeak = if (sentence.kana.isNotEmpty()) sentence.kana else sentence.japanese
                                audioManager.speakJapanese(textToSpeak)
                            }
                        } else {
                            Log.d(TAG, "No original audio path, using TTS")
                            // Fallback to TTS
                            val textToSpeak = if (sentence.kana.isNotEmpty()) sentence.kana else sentence.japanese
                            audioManager.speakJapanese(textToSpeak)
                        }
                    }
                }

                btnPlayRecording.setOnClickListener {
                    if (recordingFile.exists()) {
                        Log.d(TAG, "Playing recording: ${recording.audioPath}")
                        val success = audioManager.playAudio(recording.audioPath)
                        if (!success) {
                            Toast.makeText(root.context, "Failed to play recording", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(root.context, "Recording file not found", Toast.LENGTH_SHORT).show()
                        btnPlayRecording.isEnabled = false
                        btnPlayRecording.alpha = 0.5f
                    }
                }
            }
        }

        private fun loadSentenceData(sentenceId: Long) {
            if (sentenceCache.containsKey(sentenceId)) {
                // Use cached sentence data
                updateSentenceUI(sentenceCache[sentenceId])
                return
            }
            
            // Fetch sentence data
            coroutineScope.launch {
                try {
                    val sentence = withContext(Dispatchers.IO) {
                        repository.getSentenceById(sentenceId)
                    }
                    
                    // Cache the result
                    sentenceCache[sentenceId] = sentence
                    
                    // Update UI
                    updateSentenceUI(sentence)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading sentence: ${e.message}")
                    updateSentenceUI(null)
                }
            }
        }
        
        private fun updateSentenceUI(sentence: Sentence?) {
            binding.apply {
                if (sentence != null) {
                    japaneseText.text = sentence.japanese
                    englishText.text = sentence.english
                } else {
                    japaneseText.text = "Unknown Sentence"
                    englishText.text = "Sentence data unavailable"
                }
            }
        }

        private fun formatTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                else -> "${diff / 86400000}d ago"
            }
        }
    }

    class RecordingDiffCallback : DiffUtil.ItemCallback<Recording>() {
        override fun areItemsTheSame(oldItem: Recording, newItem: Recording): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recording, newItem: Recording): Boolean {
            return oldItem == newItem
        }
    }
}