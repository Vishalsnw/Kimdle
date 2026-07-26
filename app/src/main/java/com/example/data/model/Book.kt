package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "Unknown Author",
    val uriString: String,
    val filePath: String, // Internal app storage path copied from URI
    val totalPages: Int = 1,
    val currentPage: Int = 0, // 0-indexed page number
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val coverImagePath: String? = null, // Local thumbnail generated from page 0
    val fileSizeBytes: Long = 0L,
    val isSample: Boolean = false
) {
    val progressPercentage: Int
        get() = if (totalPages > 0) ((currentPage + 1) * 100) / totalPages else 0
        
    val formattedSize: String
        get() {
            val kb = fileSizeBytes / 1024
            return if (kb > 1024) {
                String.format("%.1f MB", kb / 1024f)
            } else {
                "${kb} KB"
            }
        }
}
