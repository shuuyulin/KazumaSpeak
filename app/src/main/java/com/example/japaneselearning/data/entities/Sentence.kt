package com.example.japaneselearning.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "sentences")
data class Sentence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val japanese: String,
    val romaji: String,
    val english: String,
    val category: String? = null,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable