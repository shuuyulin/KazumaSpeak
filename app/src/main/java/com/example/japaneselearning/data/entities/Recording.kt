package com.example.japaneselearning.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sentenceId: Long,
    val audioPath: String,
    val similarityScore: Float,
    val createdAt: Long = System.currentTimeMillis()
)