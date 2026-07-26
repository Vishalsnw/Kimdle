package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Book
import com.example.data.model.Bookmark
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.TransitionStyle
import com.example.data.repository.BookRepository
import com.example.pdf.PdfRendererHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookRepository
    private var pdfRendererHelper: PdfRendererHelper? = null
    private var bookmarkJob: Job? = null

    init {
        val dao = AppDatabase.getDatabase(application).bookDao()
        repository = BookRepository(dao)
    }

    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook

    private val _settings = MutableStateFlow(ReaderSettings())
    val settings: StateFlow<ReaderSettings> = _settings

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            // Close previous renderer if any
            pdfRendererHelper?.close()

            val book = repository.getBookByIdSync(bookId)
            if (book != null) {
                _currentBook.value = book
                _currentPageIndex.value = book.currentPage
                
                pdfRendererHelper = PdfRendererHelper(book.filePath)
                val total = pdfRendererHelper?.open() ?: 0
                if (total > 0 && book.totalPages != total) {
                    val updated = book.copy(totalPages = total)
                    _currentBook.value = updated
                }

                // Observe bookmarks
                bookmarkJob?.cancel()
                bookmarkJob = viewModelScope.launch {
                    repository.getBookmarks(bookId).collectLatest { list ->
                        _bookmarks.value = list
                        checkIfCurrentPageBookmarked(list, _currentPageIndex.value)
                    }
                }
            }
        }
    }

    private fun checkIfCurrentPageBookmarked(list: List<Bookmark>, page: Int) {
        _isBookmarked.value = list.any { it.pageNumber == page }
    }

    fun onPageChanged(newIndex: Int) {
        if (_currentPageIndex.value != newIndex) {
            _currentPageIndex.value = newIndex
            checkIfCurrentPageBookmarked(_bookmarks.value, newIndex)
            val book = _currentBook.value
            if (book != null) {
                viewModelScope.launch {
                    repository.updateProgress(book.id, newIndex)
                }
            }
        }
    }

    fun toggleOverlay() {
        _isOverlayVisible.value = !_isOverlayVisible.value
    }

    fun setOverlayVisible(visible: Boolean) {
        _isOverlayVisible.value = visible
    }

    fun toggleBookmark(note: String = "") {
        val book = _currentBook.value ?: return
        val page = _currentPageIndex.value
        viewModelScope.launch {
            if (_isBookmarked.value) {
                repository.removeBookmark(book.id, page)
            } else {
                repository.addBookmark(book.id, page, note)
            }
        }
    }

    fun updateTheme(theme: ReaderTheme) {
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateTransitionStyle(style: TransitionStyle) {
        _settings.value = _settings.value.copy(transitionStyle = style)
    }

    fun updateCropMargins(crop: Boolean) {
        _settings.value = _settings.value.copy(cropMargins = crop)
    }

    suspend fun getPageBitmap(pageIndex: Int, width: Int, height: Int): Bitmap? {
        return pdfRendererHelper?.renderPage(pageIndex, width, height)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            pdfRendererHelper?.close()
        }
    }
}
