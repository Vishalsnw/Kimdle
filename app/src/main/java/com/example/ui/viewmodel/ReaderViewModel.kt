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
import com.example.pdf.ReflowPageContent
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

    private val _isBookLoading = MutableStateFlow(true)
    val isBookLoading: StateFlow<Boolean> = _isBookLoading

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _isBookLoading.value = true
            _currentBook.value = null
            // Close previous renderer if any
            pdfRendererHelper?.close()

            val book = repository.getBookByIdSync(bookId)
            if (book != null) {
                pdfRendererHelper = PdfRendererHelper(book.filePath)
                val total = pdfRendererHelper?.open() ?: 0
                val updated = if (total > 0 && book.totalPages != total) {
                    book.copy(totalPages = total)
                } else book

                _currentPageIndex.value = updated.currentPage
                _currentBook.value = updated
                _isBookLoading.value = false

                // Observe bookmarks
                bookmarkJob?.cancel()
                bookmarkJob = viewModelScope.launch {
                    repository.getBookmarks(bookId).collectLatest { list ->
                        _bookmarks.value = list
                        checkIfCurrentPageBookmarked(list, _currentPageIndex.value)
                    }
                }
            } else {
                _isBookLoading.value = false
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

    fun updateReadingMode(mode: com.example.data.model.ReadingMode) {
        _settings.value = _settings.value.copy(readingMode = mode)
    }

    fun updateFontSize(size: Int) {
        _settings.value = _settings.value.copy(fontSize = size.coerceIn(12, 38))
    }

    suspend fun getPageText(pageIndex: Int): String? {
        return pdfRendererHelper?.extractPageText(pageIndex)
    }

    suspend fun getReflowContent(pageIndex: Int): ReflowPageContent? {
        return pdfRendererHelper?.extractReflowContent(pageIndex)
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
