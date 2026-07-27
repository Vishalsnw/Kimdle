package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.db.BookDao
import com.example.data.model.Book
import com.example.data.model.Bookmark
import com.example.pdf.PdfRendererHelper
import com.example.pdf.SampleBookGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookRepository(private val bookDao: BookDao) {

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val bookmarkedBooks: Flow<List<Book>> = bookDao.getBookmarkedBooks()

    fun getBookById(bookId: Long): Flow<Book?> = bookDao.getBookById(bookId)

    suspend fun getBookByIdSync(bookId: Long): Book? = bookDao.getBookByIdSync(bookId)

    suspend fun updateProgress(bookId: Long, page: Int) {
        bookDao.updateReadingProgress(bookId, page, System.currentTimeMillis())
    }

    suspend fun toggleBookmarkStatus(bookId: Long, isBookmarked: Boolean) {
        bookDao.updateBookmarkedStatus(bookId, isBookmarked)
    }

    suspend fun deleteBook(book: Book) = withContext(Dispatchers.IO) {
        try {
            val file = File(book.filePath)
            if (file.exists()) {
                file.delete()
            }
            book.coverImagePath?.let {
                val coverFile = File(it)
                if (coverFile.exists()) coverFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bookDao.deleteBookById(book.id)
    }

    // Bookmark queries
    fun getBookmarks(bookId: Long): Flow<List<Bookmark>> = bookDao.getBookmarksForBook(bookId)

    suspend fun addBookmark(bookId: Long, pageNumber: Int, note: String = ""): Long {
        val existing = bookDao.getBookmarkForPage(bookId, pageNumber)
        val title = "Page ${pageNumber + 1}"
        val bookmark = if (existing != null) {
            existing.copy(note = note, timestamp = System.currentTimeMillis())
        } else {
            Bookmark(bookId = bookId, pageNumber = pageNumber, pageTitle = title, note = note)
        }
        bookDao.updateBookmarkedStatus(bookId, true)
        return bookDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(bookId: Long, pageNumber: Int) {
        bookDao.deleteBookmarkForPage(bookId, pageNumber)
        // Check if there are any remaining bookmarks for this book
        val remaining = bookDao.getBookmarksForBook(bookId).first()
        if (remaining.isEmpty()) {
            bookDao.updateBookmarkedStatus(bookId, false)
        }
    }

    suspend fun initializeSampleBooksIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val currentBooks = bookDao.getAllBooks().first()
        if (currentBooks.isNotEmpty()) {
            return@withContext
        }

        val sampleFiles = SampleBookGenerator.generateAllSampleBooks(context)
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

        val sampleMeta = listOf(
            Triple("The Digital Reader Guide", "Editorial Team", "A 5-chapter guide on custom themes, reading stats, and gestures."),
            Triple("A Scandal in Bohemia", "Arthur Conan Doyle", "Classic Sherlock Holmes mystery story formatted for digital reading."),
            Triple("The Art of Mindful Reading", "Dr. Elena Vance", "An essay on cognitive focus and screen reading comfort.")
        )

        for ((index, file) in sampleFiles.withIndex()) {
            val helper = PdfRendererHelper(file.absolutePath)
            val pages = helper.open()
            helper.close()

            val coverFile = File(coversDir, "sample_cover_${index}.jpg")
            val coverPath = PdfRendererHelper.renderThumbnail(file.absolutePath, coverFile.absolutePath)

            val meta = sampleMeta.getOrElse(index) { Triple("Sample Book ${index + 1}", "Unknown Author", "") }
            val book = Book(
                title = meta.first,
                author = meta.second,
                uriString = "file://${file.absolutePath}",
                filePath = file.absolutePath,
                totalPages = if (pages > 0) pages else 1,
                currentPage = 0,
                lastReadTimestamp = System.currentTimeMillis() - (index * 3600000L),
                coverImagePath = coverPath,
                fileSizeBytes = file.length(),
                isSample = true
            )
            bookDao.insertBook(book)
        }
    }

    suspend fun importPdfFromUri(context: Context, uri: Uri): Long? = withContext(Dispatchers.IO) {
        try {
            val booksDir = File(context.filesDir, "imported_books").apply { mkdirs() }
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()

            // Get original file name from content resolver
            var title = "Imported Book"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val displayName = cursor.getString(nameIndex)
                        if (displayName != null) {
                            title = displayName.removeSuffix(".pdf").removeSuffix(".PDF")
                        }
                    }
                }
            }

            val destFile = File(booksDir, "book_${timestamp}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (!destFile.exists() || destFile.length() == 0L) {
                if (destFile.exists()) destFile.delete()
                return@withContext null
            }

            val helper = PdfRendererHelper(destFile.absolutePath)
            val totalPages = helper.open()
            helper.close()

            if (totalPages <= 0) {
                destFile.delete()
                return@withContext null
            }

            val coverFile = File(coversDir, "cover_${timestamp}.jpg")
            val coverPath = PdfRendererHelper.renderThumbnail(destFile.absolutePath, coverFile.absolutePath)

            val book = Book(
                title = title,
                author = "My Imported Document",
                uriString = uri.toString(),
                filePath = destFile.absolutePath,
                totalPages = totalPages,
                currentPage = 0,
                lastReadTimestamp = timestamp,
                coverImagePath = coverPath,
                fileSizeBytes = destFile.length(),
                isSample = false
            )
            val bookId = bookDao.insertBook(book)
            bookId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
