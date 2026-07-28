package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Book
import com.example.data.model.DailyReadingStat
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryFilter {
    ALL,
    RECENT,
    BOOKMARKED,
    SAMPLES
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookRepository

    init {
        val dao = AppDatabase.getDatabase(application).bookDao()
        repository = BookRepository(dao)
        viewModelScope.launch {
            repository.initializeSampleBooksIfNeeded(application)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    val books: StateFlow<List<Book>> = combine(
        repository.allBooks,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        var result = list
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { it.title.lowercase().contains(q) || it.author.lowercase().contains(q) }
        }
        when (filter) {
            LibraryFilter.ALL -> result
            LibraryFilter.RECENT -> result.filter { it.currentPage > 0 }.sortedByDescending { it.lastReadTimestamp }
            LibraryFilter.BOOKMARKED -> result.filter { it.isBookmarked }
            LibraryFilter.SAMPLES -> result.filter { it.isSample }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: LibraryFilter) {
        _selectedFilter.value = filter
    }

    fun importPdf(context: Context, uri: Uri, onComplete: (Long?) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            val bookId = repository.importPdfFromUri(context, uri)
            _isImporting.value = false
            onComplete(bookId)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    val dailyStats: StateFlow<List<DailyReadingStat>> = repository.allDailyStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun simulateHabitActivity(pages: Int = 10, minutes: Int = 15, words: Int = 3500) {
        viewModelScope.launch {
            repository.logReadingProgress(pagesAdded = pages, timeMinutesAdded = minutes, wordsAdded = words, rsvpUsed = true)
        }
    }

    suspend fun getBookmarksForBook(bookId: Long): List<com.example.data.model.Bookmark> {
        return repository.getBookmarks(bookId).first()
    }
}
