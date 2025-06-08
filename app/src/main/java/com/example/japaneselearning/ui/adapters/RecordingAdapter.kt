package com.example.japaneselearning.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingAdapter(
    private val repository: SentenceRepository,
    private val audioManager: AudioManager,
    private val onDeleteClick: (Recording) -> Unit
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

    inner class RecordingViewHolder(private val binding: ItemRecordingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: Recording) {
            binding.apply {
                // Fix the scoreText reference - use tvScore instead
                tvScore.text = "Score: ${recording.similarityScore.toInt()}%"

                // Set date text
                tvDate.text = formatDate(recording.createdAt)

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

                // Make sure the button text is "Record"
                btnPlayRecording.text = "Record"

                // Ensure delete button is properly set up
                btnDeleteRecording.setOnClickListener {
                    val context = root.context
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Delete Recording")
                        .setMessage("Are you sure you want to delete this recording?")
                        .setPositiveButton("Delete") { _, _ ->
                            onDeleteClick(recording)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
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

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
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