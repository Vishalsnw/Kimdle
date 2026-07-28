package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_reading_stats")
data class DailyReadingStat(
    @PrimaryKey val dateString: String, // Format YYYY-MM-DD
    val pagesRead: Int = 0,
    val readingTimeMinutes: Int = 0,
    val wordsRead: Int = 0,
    val rsvpUsed: Boolean = false,
    val nightRead: Boolean = false
)
