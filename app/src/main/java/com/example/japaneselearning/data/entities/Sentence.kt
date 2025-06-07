package com.example.japaneselearning.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "sentences")
data class Sentence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val japanese: String,        // Japanese with Kanji (e.g., "今日は良い天気ですね")
    val kana: String,           // Japanese without Kanji - Kana only (e.g., "きょうはいいてんきですね")
    val romaji: String,         // Romaji version (e.g., "kyou wa ii tenki desu ne")
    val english: String,        // English translation
    val category: String? = null,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable