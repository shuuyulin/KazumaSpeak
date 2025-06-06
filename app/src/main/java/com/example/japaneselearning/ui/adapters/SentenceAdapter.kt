// app/src/main/java/com/example/japaneselearning/ui/adapters/SentenceAdapter.kt
package com.example.japaneselearning.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
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
                romajiText.text = sentence.romaji
                englishText.text = sentence.english
                categoryChip.text = sentence.category ?: "General"
                
                btnPlayAudio.setOnClickListener {
                    // Play audio for this sentence
                    // TODO: Implement audio playback
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