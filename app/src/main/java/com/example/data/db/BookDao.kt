package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Book
import com.example.data.model.Bookmark
import com.example.data.model.DailyReadingStat
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE isBookmarked = 1 ORDER BY lastReadTimestamp DESC")
    fun getBookmarkedBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun getBookById(bookId: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookByIdSync(bookId: Long): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Query("UPDATE books SET currentPage = :page, lastReadTimestamp = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, page: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET isBookmarked = :isBookmarked WHERE id = :bookId")
    suspend fun updateBookmarkedStatus(bookId: Long, isBookmarked: Boolean)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    // Bookmarks queries
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getBookmarkForPage(bookId: Long, pageNumber: Int): Bookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkForPage(bookId: Long, pageNumber: Int)

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: Long)

    // Daily Reading Stats queries
    @Query("SELECT * FROM daily_reading_stats ORDER BY dateString ASC")
    fun getAllDailyStats(): Flow<List<DailyReadingStat>>

    @Query("SELECT * FROM daily_reading_stats WHERE dateString = :dateString LIMIT 1")
    suspend fun getDailyStatByDate(dateString: String): DailyReadingStat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStat(stat: DailyReadingStat): Long
}
