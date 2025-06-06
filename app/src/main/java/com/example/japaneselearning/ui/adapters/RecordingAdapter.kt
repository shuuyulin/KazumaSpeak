package com.example.japaneselearning.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.japaneselearning.R
import com.example.japaneselearning.data.entities.Recording
import com.example.japaneselearning.databinding.ItemRecordingBinding

class RecordingAdapter : ListAdapter<Recording, RecordingAdapter.RecordingViewHolder>(RecordingDiffCallback()) {

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
                // For now, we'll show placeholder text since we don't have sentence data
                // In a real app, you'd fetch the sentence data using recording.sentenceId
                japaneseText.text = "Japanese Text" // TODO: Fetch from sentence
                englishText.text = "English Text"   // TODO: Fetch from sentence
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

                btnPlayOriginal.setOnClickListener {
                    // Play original audio
                }

                btnPlayRecording.setOnClickListener {
                    // Play user recording
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