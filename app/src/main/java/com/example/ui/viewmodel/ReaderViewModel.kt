package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VolumeNavDirection { NEXT, PREVIOUS }

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BookRepository
    private var pdfRendererHelper: PdfRendererHelper? = null
    private var bookmarkJob: Job? = null

    private var tts: TextToSpeech? = null
    private var currentSentences = listOf<String>()
    private var currentSentenceIndex = 0
    private var isTtsActive = false

    init {
        val dao = AppDatabase.getDatabase(application).bookDao()
        repository = BookRepository(dao)
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                _isTtsReady.value = true
                setupTtsListener()
            }
        }
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

    private val _isTtsPlaying = MutableStateFlow(false)
    val isTtsPlaying: StateFlow<Boolean> = _isTtsPlaying

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady

    private val _ttsSpeed = MutableStateFlow(1.0f)
    val ttsSpeed: StateFlow<Float> = _ttsSpeed

    private val _ttsPitch = MutableStateFlow(1.0f)
    val ttsPitch: StateFlow<Float> = _ttsPitch

    private val _currentTtsSentence = MutableStateFlow("")
    val currentTtsSentence: StateFlow<String> = _currentTtsSentence

    private val _ttsProgress = MutableStateFlow<Pair<Int, Int>>(0 to 0)
    val ttsProgress: StateFlow<Pair<Int, Int>> = _ttsProgress

    private val _isRsvpPlaying = MutableStateFlow(false)
    val isRsvpPlaying: StateFlow<Boolean> = _isRsvpPlaying

    private val _rsvpWpm = MutableStateFlow(300)
    val rsvpWpm: StateFlow<Int> = _rsvpWpm

    private val _rsvpCurrentWordIndex = MutableStateFlow(0)
    val rsvpCurrentWordIndex: StateFlow<Int> = _rsvpCurrentWordIndex

    private val _rsvpWords = MutableStateFlow<List<String>>(emptyList())
    val rsvpWords: StateFlow<List<String>> = _rsvpWords

    private val _volumeNavEvent = MutableSharedFlow<VolumeNavDirection>(extraBufferCapacity = 5)
    val volumeNavEvent: SharedFlow<VolumeNavDirection> = _volumeNavEvent

    private val _isAutoScrollActive = MutableStateFlow(false)
    val isAutoScrollActive: StateFlow<Boolean> = _isAutoScrollActive

    private val _autoScrollSpeed = MutableStateFlow(5) // 1..10
    val autoScrollSpeed: StateFlow<Int> = _autoScrollSpeed

    private var rsvpJob: Job? = null
    var isRsvpActive = false

    fun loadBook(bookId: Long) {
        if (_currentBook.value?.id == bookId && !_isBookLoading.value) {
            return
        }
        viewModelScope.launch {
            _isBookLoading.value = true
            stopTts()
            stopRsvp()
            stopAutoScroll()

            val book = repository.getBookByIdSync(bookId)
            if (book != null) {
                pdfRendererHelper?.close()
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
                    repository.logReadingProgress(pagesAdded = 1, timeMinutesAdded = 1, rsvpUsed = isRsvpActive)
                }
            }
            if (isTtsActive) {
                tts?.stop()
                viewModelScope.launch {
                    loadPageSentencesAndSpeak(newIndex, startFromBeginning = true)
                }
            } else {
                currentSentences = emptyList()
                currentSentenceIndex = 0
                _currentTtsSentence.value = ""
                _ttsProgress.value = 0 to 0
            }
            if (isRsvpActive) {
                rsvpJob?.cancel()
                viewModelScope.launch {
                    loadRsvpWordsAndPlay(newIndex, startFromBeginning = true)
                }
            } else {
                _rsvpWords.value = emptyList()
                _rsvpCurrentWordIndex.value = 0
            }
        }
    }

    fun toggleOverlay() {
        _isOverlayVisible.value = !_isOverlayVisible.value
    }

    fun setOverlayVisible(visible: Boolean) {
        _isOverlayVisible.value = visible
    }

    fun toggleBookmark(
        note: String = "",
        color: String = "Yellow",
        selectedText: String = ""
    ) {
        val book = _currentBook.value ?: return
        val page = _currentPageIndex.value
        viewModelScope.launch {
            if (_isBookmarked.value && note.isEmpty() && selectedText.isEmpty()) {
                repository.removeBookmark(book.id, page)
            } else {
                repository.addBookmark(
                    bookId = book.id,
                    pageNumber = page,
                    note = note,
                    highlightColor = color,
                    selectedText = selectedText
                )
            }
        }
    }

    fun deleteBookmarkById(bookmarkId: Long) {
        val book = _currentBook.value ?: return
        viewModelScope.launch {
            repository.deleteBookmarkById(book.id, bookmarkId)
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

    fun updateBionicReading(enabled: Boolean) {
        _settings.value = _settings.value.copy(bionicReading = enabled)
    }

    fun updateFontSize(size: Int) {
        _settings.value = _settings.value.copy(fontSize = size.coerceIn(12, 38))
    }

    fun updateVolumeKeyNavigation(enabled: Boolean) {
        _settings.value = _settings.value.copy(volumeKeyNavigation = enabled)
    }

    fun isVolumeKeyNavEnabled(): Boolean {
        return _settings.value.volumeKeyNavigation
    }

    fun onVolumeDownPressed(): Boolean {
        if (_settings.value.volumeKeyNavigation) {
            _volumeNavEvent.tryEmit(VolumeNavDirection.NEXT)
            return true
        }
        return false
    }

    fun onVolumeUpPressed(): Boolean {
        if (_settings.value.volumeKeyNavigation) {
            _volumeNavEvent.tryEmit(VolumeNavDirection.PREVIOUS)
            return true
        }
        return false
    }

    fun onVolumeKeyDown(): Boolean = onVolumeDownPressed()
    fun onVolumeKeyUp(): Boolean = onVolumeUpPressed()

    fun startAutoScroll() {
        stopTts()
        stopRsvp()
        _isAutoScrollActive.value = true
    }

    fun stopAutoScroll() {
        _isAutoScrollActive.value = false
    }

    fun toggleAutoScroll() {
        if (_isAutoScrollActive.value) {
            stopAutoScroll()
        } else {
            startAutoScroll()
        }
    }

    fun setAutoScrollSpeed(speed: Int) {
        val coerced = speed.coerceIn(1, 10)
        _autoScrollSpeed.value = coerced
        _settings.value = _settings.value.copy(autoScrollSpeed = coerced)
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

    private suspend fun loadPageSentencesAndSpeak(pageIndex: Int, startFromBeginning: Boolean) {
        val content = getReflowContent(pageIndex)
        val rawText = content?.text?.trim() ?: ""
        if (rawText.isEmpty()) {
            _currentTtsSentence.value = "No text on page ${pageIndex + 1}."
            if (isTtsActive) {
                kotlinx.coroutines.delay(1500)
                if (isTtsActive && _isTtsPlaying.value) {
                    val book = _currentBook.value
                    if (book != null && pageIndex + 1 < book.totalPages) {
                        onPageChanged(pageIndex + 1)
                    } else {
                        stopTts()
                    }
                }
            }
            return
        }

        val cleanedText = rawText
            .replace(Regex("(\\w+)-\\r?\\n(\\w+)"), "$1$2")
            .replace(Regex("\\r?\\n"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val rawSentences = cleanedText.split(Regex("(?<=[.!?；;])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val chunks = mutableListOf<String>()
        for (s in rawSentences) {
            if (s.length <= 350) {
                chunks.add(s)
            } else {
                val subParts = s.split(Regex("(?<=[,،])\\s+"))
                var currentChunk = ""
                for (part in subParts) {
                    if ((currentChunk + " " + part).length <= 350) {
                        currentChunk = if (currentChunk.isEmpty()) part else "$currentChunk $part"
                    } else {
                        if (currentChunk.isNotEmpty()) chunks.add(currentChunk.trim())
                        if (part.length <= 350) {
                            currentChunk = part
                        } else {
                            chunks.addAll(part.chunked(300))
                            currentChunk = ""
                        }
                    }
                }
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.trim())
                }
            }
        }

        currentSentences = if (chunks.isNotEmpty()) chunks else listOf(cleanedText)
        currentSentenceIndex = if (startFromBeginning) 0 else currentSentenceIndex.coerceIn(0, currentSentences.size - 1)

        speakCurrentSentence()
    }

    private fun speakCurrentSentence() {
        if (!isTtsActive || !_isTtsPlaying.value) return
        if (currentSentenceIndex < 0 || currentSentenceIndex >= currentSentences.size) {
            speakNextSentence()
            return
        }
        val textToSpeak = currentSentences[currentSentenceIndex]
        _currentTtsSentence.value = textToSpeak
        _ttsProgress.value = (currentSentenceIndex + 1) to currentSentences.size

        val params = Bundle()
        val utteranceId = "TTS_SENTENCE_$currentSentenceIndex"
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts?.setSpeechRate(_ttsSpeed.value)
        tts?.setPitch(_ttsPitch.value)
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun speakNextSentence() {
        if (!isTtsActive || !_isTtsPlaying.value) return
        currentSentenceIndex++
        if (currentSentenceIndex < currentSentences.size) {
            speakCurrentSentence()
        } else {
            val book = _currentBook.value
            val total = book?.totalPages ?: 1
            val nextPage = _currentPageIndex.value + 1
            if (nextPage < total) {
                onPageChanged(nextPage)
            } else {
                _currentTtsSentence.value = "Finished reading book."
                stopTts()
            }
        }
    }

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                viewModelScope.launch {
                    if (isTtsActive && _isTtsPlaying.value) {
                        speakNextSentence()
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                viewModelScope.launch {
                    _isTtsPlaying.value = false
                    isTtsActive = false
                }
            }
        })
    }

    fun toggleTts() {
        if (!_isTtsReady.value) return
        if (_isTtsPlaying.value) {
            pauseTts()
        } else {
            startTts()
        }
    }

    fun startTts() {
        if (!_isTtsReady.value) return
        _isTtsPlaying.value = true
        isTtsActive = true
        viewModelScope.launch {
            if (currentSentences.isEmpty() || currentSentenceIndex >= currentSentences.size) {
                loadPageSentencesAndSpeak(_currentPageIndex.value, startFromBeginning = true)
            } else {
                speakCurrentSentence()
            }
        }
    }

    fun pauseTts() {
        _isTtsPlaying.value = false
        isTtsActive = false
        tts?.stop()
    }

    fun stopTts() {
        _isTtsPlaying.value = false
        isTtsActive = false
        tts?.stop()
        _currentTtsSentence.value = ""
        currentSentenceIndex = 0
        currentSentences = emptyList()
        _ttsProgress.value = 0 to 0
    }

    fun setTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
        tts?.setSpeechRate(speed)
        if (_isTtsPlaying.value && isTtsActive) {
            speakCurrentSentence()
        }
    }

    fun setTtsPitch(pitch: Float) {
        _ttsPitch.value = pitch
        tts?.setPitch(pitch)
        if (_isTtsPlaying.value && isTtsActive) {
            speakCurrentSentence()
        }
    }

    fun skipTtsPreviousSentence() {
        if (!isTtsActive) return
        if (currentSentenceIndex > 0) {
            currentSentenceIndex--
            speakCurrentSentence()
        } else if (_currentPageIndex.value > 0) {
            onPageChanged(_currentPageIndex.value - 1)
        }
    }

    fun skipTtsNextSentence() {
        if (!isTtsActive) return
        speakNextSentence()
    }

    private suspend fun loadRsvpWordsAndPlay(pageIndex: Int, startFromBeginning: Boolean) {
        val content = getReflowContent(pageIndex)
        val rawText = content?.text?.trim() ?: ""
        if (rawText.isEmpty()) {
            _rsvpWords.value = listOf("No", "text", "on", "page", "${pageIndex + 1}.")
            _rsvpCurrentWordIndex.value = 0
            if (isRsvpActive && _isRsvpPlaying.value) {
                kotlinx.coroutines.delay(1500)
                val book = _currentBook.value
                if (book != null && pageIndex + 1 < book.totalPages) {
                    onPageChanged(pageIndex + 1)
                } else {
                    stopRsvp()
                }
            }
            return
        }

        val cleaned = rawText
            .replace(Regex("(\\w+)-\\r?\\n(\\w+)"), "$1$2")
            .replace(Regex("\\r?\\n"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val words = cleaned.split(" ").filter { it.isNotEmpty() }
        _rsvpWords.value = words
        _rsvpCurrentWordIndex.value = if (startFromBeginning) 0 else _rsvpCurrentWordIndex.value.coerceIn(0, (words.size - 1).coerceAtLeast(0))

        viewModelScope.launch {
            repository.logReadingProgress(wordsAdded = words.size, rsvpUsed = true)
        }

        if (isRsvpActive && _isRsvpPlaying.value) {
            startRsvpLoop()
        }
    }

    private fun startRsvpLoop() {
        rsvpJob?.cancel()
        rsvpJob = viewModelScope.launch {
            while (_isRsvpPlaying.value && isRsvpActive) {
                val words = _rsvpWords.value
                val index = _rsvpCurrentWordIndex.value
                if (words.isEmpty() || index >= words.size) {
                    val book = _currentBook.value
                    val total = book?.totalPages ?: 1
                    val nextPage = _currentPageIndex.value + 1
                    if (nextPage < total) {
                        onPageChanged(nextPage)
                    } else {
                        stopRsvp()
                    }
                    break
                }

                val currentWord = words[index]
                val baseDelayMs = (60000L / _rsvpWpm.value.coerceAtLeast(100))
                val extraDelay = when {
                    currentWord.endsWith(".") || currentWord.endsWith("!") || currentWord.endsWith("?") -> baseDelayMs * 2
                    currentWord.endsWith(",") || currentWord.endsWith(";") || currentWord.endsWith(":") -> baseDelayMs / 2
                    else -> 0L
                }
                kotlinx.coroutines.delay(baseDelayMs + extraDelay)
                _rsvpCurrentWordIndex.value = index + 1
            }
        }
    }

    fun toggleRsvp() {
        if (_isRsvpPlaying.value) {
            pauseRsvp()
        } else {
            startRsvp()
        }
    }

    fun startRsvp() {
        _isRsvpPlaying.value = true
        isRsvpActive = true
        if (_rsvpWords.value.isEmpty() || _rsvpCurrentWordIndex.value >= _rsvpWords.value.size) {
            viewModelScope.launch {
                loadRsvpWordsAndPlay(_currentPageIndex.value, startFromBeginning = true)
            }
        } else {
            startRsvpLoop()
        }
    }

    fun pauseRsvp() {
        _isRsvpPlaying.value = false
        isRsvpActive = false
        rsvpJob?.cancel()
    }

    fun stopRsvp() {
        _isRsvpPlaying.value = false
        isRsvpActive = false
        rsvpJob?.cancel()
        _rsvpCurrentWordIndex.value = 0
        _rsvpWords.value = emptyList()
    }

    fun setRsvpWpm(wpm: Int) {
        _rsvpWpm.value = wpm.coerceIn(100, 1000)
    }

    fun skipRsvpPrevious() {
        if (!isRsvpActive) return
        val newIndex = (_rsvpCurrentWordIndex.value - 10).coerceAtLeast(0)
        _rsvpCurrentWordIndex.value = newIndex
    }

    fun skipRsvpNext() {
        if (!isRsvpActive) return
        val words = _rsvpWords.value
        val newIndex = (_rsvpCurrentWordIndex.value + 10).coerceAtMost(words.size - 1)
        if (newIndex >= 0) {
            _rsvpCurrentWordIndex.value = newIndex
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
        stopRsvp()
        tts?.shutdown()
        viewModelScope.launch {
            pdfRendererHelper?.close()
        }
    }
}
