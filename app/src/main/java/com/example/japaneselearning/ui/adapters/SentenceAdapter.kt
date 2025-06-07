// app/src/main/java/com/example/japaneselearning/ui/adapters/SentenceAdapter.kt
package com.example.japaneselearning.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.databinding.ItemSentenceBinding

class SentenceAdapter(
    private val onEditClick: (Sentence) -> Unit
) : ListAdapter<Sentence, SentenceAdapter.SentenceViewHolder>(SentenceDiffCallback()) {

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
                japaneseText.text = sentence.japanese
                kanaText.text = sentence.kana
                romajiText.text = sentence.romaji
                englishText.text = sentence.english
                categoryChip.text = sentence.category ?: "General"
                
                btnPlayAudio.setOnClickListener {
                    val context = binding.root.context
                    val audioManager = com.example.japaneselearning.utils.AudioManager(context)
                    
                    try {
                        var audioPlayed = false
                        
                        // Try to play audio file first
                        if (!sentence.audioPath.isNullOrEmpty()) {
                            Log.d("SentenceAdapter", "Attempting to play audio file: ${sentence.audioPath}")
                            audioPlayed = audioManager.playAudio(sentence.audioPath)
                        }
                        
                        // Fallback to TTS if no audio file or audio file failed
                        if (!audioPlayed) {
                            Log.d("SentenceAdapter", "Audio file failed, using TTS")
                            // Use kana version if available, otherwise japanese
                            val textToSpeak = if (sentence.kana.isNotEmpty()) sentence.kana else sentence.japanese
                            val ttsSuccess = audioManager.speakJapanese(textToSpeak)
                            
                            if (!ttsSuccess) {
                                // Show error message if both audio file and TTS fail
                                android.widget.Toast.makeText(
                                    context, 
                                    "Unable to play audio", 
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SentenceAdapter", "Error in play audio: ${e.message}", e)
                        android.widget.Toast.makeText(
                            context, 
                            "Audio playback failed", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                btnEdit.setOnClickListener {
                    onEditClick(sentence)
                }
            }
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