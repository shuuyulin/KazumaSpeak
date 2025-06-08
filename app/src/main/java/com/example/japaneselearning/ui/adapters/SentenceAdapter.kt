// app/src/main/java/com/example/japaneselearning/ui/adapters/SentenceAdapter.kt
package com.example.japaneselearning.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.databinding.ItemSentenceBinding
import com.example.japaneselearning.utils.AudioManager
import java.io.File

class SentenceAdapter(
    private val audioManager: AudioManager,  // Add AudioManager parameter
    private val onEditClick: (Sentence) -> Unit,
    private val onDeleteClick: (Sentence) -> Unit
) : ListAdapter<Sentence, SentenceAdapter.SentenceViewHolder>(SentenceDiffCallback()) {

    private val TAG = "SentenceAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SentenceViewHolder {
        val binding = ItemSentenceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SentenceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SentenceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SentenceViewHolder(
        private val binding: ItemSentenceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sentence: Sentence) {
            binding.apply {
                // Set text values
                tvJapanese.text = sentence.japanese
                tvKana.text = sentence.kana.takeIf { !it.isNullOrEmpty() } ?: ""
                tvEnglish.text = sentence.english
                tvCategory.text = sentence.category ?: "General"
                
                // Setup play button click listener
                btnPlayAudio.setOnClickListener {
                    playAudio(sentence)
                }
                
                // Setup edit button click listener
                btnEditSentence.setOnClickListener {
                    onEditClick(sentence)
                }

                // Setup delete button click listener
                btnDeleteSentence.setOnClickListener {
                    val context = root.context
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Delete Sentence")
                        .setMessage("Are you sure you want to delete this sentence? This will also delete all recordings associated with it.")
                        .setPositiveButton("Delete") { _, _ ->
                            onDeleteClick(sentence)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
        
        private fun playAudio(sentence: Sentence) {
            // Check if there's an audio file and try to play it
            if (!sentence.audioPath.isNullOrEmpty()) {
                val audioFile = File(sentence.audioPath)
                if (audioFile.exists()) {
                    Log.d(TAG, "Playing audio file: ${sentence.audioPath}")
                    val success = audioManager.playAudio(sentence.audioPath)
                    if (success) {
                        return
                    } else {
                        Log.e(TAG, "Failed to play audio file: ${sentence.audioPath}")
                        Toast.makeText(itemView.context, "Failed to play audio", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "Audio file does not exist: ${sentence.audioPath}")
                }
            }

            // If no audio file or playback failed, use TTS
            Log.d(TAG, "Using TTS to speak Japanese text")
            val textToSpeak = if (!sentence.kana.isNullOrEmpty()) sentence.kana else sentence.japanese
            audioManager.speakJapanese(textToSpeak)
        }
    }

    class SentenceDiffCallback : DiffUtil.ItemCallback<Sentence>() {
        override fun areItemsTheSame(oldItem: Sentence, newItem: Sentence): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Sentence, newItem: Sentence): Boolean {
            return oldItem == newItem
        }
    }
}