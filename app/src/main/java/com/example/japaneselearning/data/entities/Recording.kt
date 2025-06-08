package com.example.japaneselearning.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = Sentence::class,
            parentColumns = ["id"],
            childColumns = ["sentenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Recording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sentenceId: Long,
    val audioPath: String,
    val similarityScore: Float,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable