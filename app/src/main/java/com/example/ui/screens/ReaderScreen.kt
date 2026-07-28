package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.geometry.Offset
import com.example.data.model.ReadingMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Book
import com.example.data.model.Bookmark
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.TransitionStyle
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.LightPageBg
import com.example.ui.theme.NightPageBg
import com.example.ui.theme.SepiaPageBg
import com.example.ui.theme.SepiaPageText
import com.example.ui.theme.WarmCharcoal
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val book by viewModel.currentBook.collectAsStateWithLifecycle()
    val isBookLoading by viewModel.isBookLoading.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isOverlayVisible by viewModel.isOverlayVisible.collectAsStateWithLifecycle()
    val currentPageIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val isRsvpPlaying by viewModel.isRsvpPlaying.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }
    var showAudiobookBar by remember { mutableStateOf(false) }
    var showRsvpBar by remember { mutableStateOf(false) }
    var currentSubPage by remember { mutableIntStateOf(0) }
    var totalSubPages by remember { mutableIntStateOf(1) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    BackHandler {
        if (showRsvpBar || isRsvpPlaying) {
            viewModel.stopRsvp()
            showRsvpBar = false
        } else if (showAudiobookBar || isTtsPlaying) {
            viewModel.stopTts()
            showAudiobookBar = false
        } else if (isOverlayVisible) {
            viewModel.setOverlayVisible(false)
        } else {
            onBack()
        }
    }

    val currentThemeBg = when (settings.theme) {
        ReaderTheme.LIGHT -> LightPageBg
        ReaderTheme.SEPIA -> SepiaPageBg
        ReaderTheme.NIGHT -> NightPageBg
    }

    val currentThemeText = when (settings.theme) {
        ReaderTheme.LIGHT -> Color.Black
        ReaderTheme.SEPIA -> SepiaPageText
        ReaderTheme.NIGHT -> Color(0xFFE0D8C8)
    }

    // Color matrix for PDF bitmap tinting
    val pdfColorFilter = remember(settings.theme) {
        when (settings.theme) {
            ReaderTheme.LIGHT -> null
            ReaderTheme.SEPIA -> {
                // Sepia tint matrix that shifts white background to warm sepia (#F4ECD8) and black ink to brown (#4A3B32)
                val sepiaMatrix = ColorMatrix(
                    floatArrayOf(
                        0.90f, 0.05f, 0.00f, 0f, 15f,
                        0.05f, 0.85f, 0.00f, 0f, 10f,
                        0.00f, 0.00f, 0.75f, 0f, 0f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(sepiaMatrix)
            }
            ReaderTheme.NIGHT -> {
                // Night mode inversion matrix: White becomes #121212, Black becomes #E0D8C8
                val nightMatrix = ColorMatrix(
                    floatArrayOf(
                        -0.88f, 0.00f, 0.00f, 0f, 224f,
                        0.00f, -0.85f, 0.00f, 0f, 216f,
                        0.00f, 0.00f, -0.80f, 0f, 200f,
                        0.00f, 0.00f, 0.00f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(nightMatrix)
            }
        }
    }

    val totalPages = book?.totalPages ?: 1
    val pagerState = rememberPagerState(initialPage = currentPageIndex) { totalPages }

    var previousPageIndex by remember { mutableIntStateOf(currentPageIndex) }
    val isMovingBackwards = remember(currentPageIndex, previousPageIndex) {
        currentPageIndex < previousPageIndex
    }
    LaunchedEffect(currentPageIndex) {
        previousPageIndex = currentPageIndex
    }

    // Sync pager with ViewModel state
    LaunchedEffect(currentPageIndex) {
        if (pagerState.currentPage != currentPageIndex && currentPageIndex < totalPages) {
            pagerState.scrollToPage(currentPageIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentThemeBg)
    ) {
        if (book == null || isBookLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AmberPrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Opening Book...", style = MaterialTheme.typography.bodyMedium, color = currentThemeText.copy(alpha = 0.7f))
                }
            }
        } else {
            // Reader Content Canvas
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val screenWidthPx = remember(configuration) {
                with(density) { configuration.screenWidthDp.dp.roundToPx() }
            }
            val screenHeightPx = remember(configuration) {
                with(density) { configuration.screenHeightDp.dp.roundToPx() }
            }

            val onNextPdfPage = {
                if (pagerState.currentPage < totalPages - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }
            val onPrevPdfPage = {
                if (pagerState.currentPage > 0) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }
            }

            if (settings.readingMode == ReadingMode.SMART_REFLOW || settings.transitionStyle == TransitionStyle.HORIZONTAL_FLIP) {
                // Horizontal Page Flip & Reflow Mode
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(settings.readingMode) {
                            if (settings.readingMode != ReadingMode.SMART_REFLOW) {
                                detectTapGestures { offset ->
                                    val width = size.width
                                    when {
                                        offset.x < width * 0.22f -> onPrevPdfPage()
                                        offset.x > width * 0.78f -> onNextPdfPage()
                                        else -> viewModel.toggleOverlay()
                                    }
                                }
                            }
                        }
                ) { pageIndex ->
                    ReaderPageContent(
                        pageIndex = pageIndex,
                        settings = settings,
                        widthPx = screenWidthPx,
                        heightPx = screenHeightPx,
                        colorFilter = pdfColorFilter,
                        viewModel = viewModel,
                        isMovingBackwards = isMovingBackwards,
                        onNextPdfPage = onNextPdfPage,
                        onPrevPdfPage = onPrevPdfPage,
                        onToggleOverlay = { viewModel.toggleOverlay() },
                        onSubPageChanged = { current, total ->
                            if (pageIndex == currentPageIndex) {
                                currentSubPage = current
                                totalSubPages = total
                            }
                        }
                    )
                }
            } else {
                // Vertical Continuous Scroll Mode
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val width = size.width
                                if (offset.x > width * 0.22f && offset.x < width * 0.78f) {
                                    viewModel.toggleOverlay()
                                }
                            }
                        }
                ) {
                    items(count = totalPages) { pageIndex ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(configuration.screenHeightDp.dp)
                        ) {
                            ReaderPageContent(
                                pageIndex = pageIndex,
                                settings = settings,
                                widthPx = screenWidthPx,
                                heightPx = screenHeightPx,
                                colorFilter = pdfColorFilter,
                                viewModel = viewModel,
                                isMovingBackwards = isMovingBackwards,
                                onNextPdfPage = onNextPdfPage,
                                onPrevPdfPage = onPrevPdfPage,
                                onToggleOverlay = { viewModel.toggleOverlay() },
                                onSubPageChanged = { current, total ->
                                    if (pageIndex == currentPageIndex) {
                                        currentSubPage = current
                                        totalSubPages = total
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Top Toolbar Overlay
            AnimatedVisibility(
                visible = isOverlayVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            text = book?.title ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Row {
                            // Speed Reading (RSVP Mode) Button
                            IconButton(onClick = {
                                if (isRsvpPlaying || showRsvpBar) {
                                    viewModel.stopRsvp()
                                    showRsvpBar = false
                                } else {
                                    if (isTtsPlaying || showAudiobookBar) {
                                        viewModel.stopTts()
                                        showAudiobookBar = false
                                    }
                                    showRsvpBar = true
                                    viewModel.startRsvp()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "RSVP Speed Reading Mode",
                                    tint = if (isRsvpPlaying || showRsvpBar) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Audiobook Mode Button
                            IconButton(onClick = {
                                if (isTtsPlaying || showAudiobookBar) {
                                    viewModel.stopTts()
                                    showAudiobookBar = false
                                } else {
                                    if (isRsvpPlaying || showRsvpBar) {
                                        viewModel.stopRsvp()
                                        showRsvpBar = false
                                    }
                                    showAudiobookBar = true
                                    viewModel.startTts()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isTtsPlaying) Icons.Default.VolumeUp else Icons.Default.Headset,
                                    contentDescription = "Audiobook Mode",
                                    tint = if (isTtsPlaying || showAudiobookBar) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Smart Reflow Toggle Button
                            IconButton(onClick = {
                                val newMode = if (settings.readingMode == ReadingMode.SMART_REFLOW) ReadingMode.ORIGINAL_LAYOUT else ReadingMode.SMART_REFLOW
                                viewModel.updateReadingMode(newMode)
                            }) {
                                Icon(
                                    imageVector = if (settings.readingMode == ReadingMode.SMART_REFLOW) Icons.Default.TextFields else Icons.Default.Image,
                                    contentDescription = "Toggle Smart Reflow Mode",
                                    tint = if (settings.readingMode == ReadingMode.SMART_REFLOW) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Bookmark Button
                            IconButton(onClick = {
                                if (isBookmarked) {
                                    viewModel.toggleBookmark()
                                } else {
                                    bookmarkNoteText = ""
                                    showAddBookmarkDialog = true
                                }
                            }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark Page",
                                    tint = if (isBookmarked) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Bookmarks List Button
                            IconButton(onClick = { showBookmarksSheet = true }) {
                                Icon(Icons.Default.Bookmarks, contentDescription = "All Bookmarks", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            // Settings Button
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Reader Settings", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Bottom Overlays Container (Audiobook Player & Navigation Overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // RSVP Speed Reading Card
                AnimatedVisibility(
                    visible = showRsvpBar || isRsvpPlaying,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    RsvpPlayerCard(
                        viewModel = viewModel,
                        onClose = {
                            viewModel.stopRsvp()
                            showRsvpBar = false
                        }
                    )
                }

                // Audiobook Player Card
                AnimatedVisibility(
                    visible = showAudiobookBar || isTtsPlaying,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    AudiobookPlayerCard(
                        viewModel = viewModel,
                        onClose = {
                            viewModel.stopTts()
                            showAudiobookBar = false
                        }
                    )
                }

                // Bottom Navigation Overlay
                AnimatedVisibility(
                    visible = isOverlayVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // Page Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${currentPageIndex + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(36.dp)
                                )
                                Slider(
                                    value = currentPageIndex.toFloat(),
                                    onValueChange = { val newPage = it.toInt()
                                        if (newPage != currentPageIndex && newPage in 0 until totalPages) {
                                            scope.launch { pagerState.scrollToPage(newPage) }
                                        }
                                    },
                                    valueRange = 0f..(if (totalPages > 1) (totalPages - 1).toFloat() else 1f),
                                    steps = if (totalPages > 2) totalPages - 2 else 0,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AmberPrimary,
                                        activeTrackColor = AmberPrimary
                                    ),
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text(
                                    text = "$totalPages",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Reading Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pageDisplay = if (settings.readingMode == ReadingMode.SMART_REFLOW && totalSubPages > 1) {
                                    "Page ${currentPageIndex + 1} (${currentSubPage + 1}/$totalSubPages)"
                                } else {
                                    "Page ${currentPageIndex + 1} of $totalPages"
                                }
                                Text(
                                    text = pageDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                val progressPercent = if (totalPages > 0) ((currentPageIndex + 1) * 100) / totalPages else 0
                                Text(
                                    text = "$progressPercent% Read",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AmberPrimary)
                                )
                                val remainingPages = totalPages - (currentPageIndex + 1)
                                val estMinutes = (remainingPages * 1.5).toInt()
                                Text(
                                    text = if (estMinutes > 0) "~$estMinutes mins left" else "Finished",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Persistent reading status footer when overlay is hidden
            if (!isOverlayVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pageStr = if (settings.readingMode == ReadingMode.SMART_REFLOW && totalSubPages > 1) {
                        "Page ${currentPageIndex + 1} (${currentSubPage + 1}/$totalSubPages)"
                    } else {
                        "Page ${currentPageIndex + 1} of $totalPages"
                    }
                    Text(
                        text = pageStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = currentThemeText.copy(alpha = 0.5f)
                    )
                    val progressPercent = if (totalPages > 0) ((currentPageIndex + 1) * 100) / totalPages else 0
                    Text(
                        text = "$progressPercent% Read",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = currentThemeText.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Add Bookmark Dialog
    if (showAddBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Bookmark Page ${currentPageIndex + 1}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Add an optional study note or reminder for this page:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = bookmarkNoteText,
                        onValueChange = { bookmarkNoteText = it },
                        placeholder = { Text("e.g., Important quote about reading rituals...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleBookmark(bookmarkNoteText)
                    showAddBookmarkDialog = false
                }) {
                    Text("Save Bookmark", color = AmberPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bookmarks Bottom Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Bookmarks & Notes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No bookmarks added yet. Tap the bookmark ribbon while reading to mark a page.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bm ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { pagerState.scrollToPage(bm.pageNumber) }
                                        showBookmarksSheet = false
                                        viewModel.setOverlayVisible(false)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AmberPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${bm.pageNumber + 1}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Page ${bm.pageNumber + 1}", fontWeight = FontWeight.Bold)
                                        if (bm.note.isNotEmpty()) {
                                            Text(
                                                text = bm.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Reading Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Reading Mode Section
                Text(text = "Reading Mode (Smart Text Fit)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TransitionStyleCard(
                        title = "📖 Smart Reflow",
                        subtitle = "Auto-wraps to screen width",
                        selected = settings.readingMode == ReadingMode.SMART_REFLOW,
                        onClick = { viewModel.updateReadingMode(ReadingMode.SMART_REFLOW) },
                        modifier = Modifier.weight(1f)
                    )
                    TransitionStyleCard(
                        title = "🖼️ Original Layout",
                        subtitle = "Exact document image",
                        selected = settings.readingMode == ReadingMode.ORIGINAL_LAYOUT,
                        onClick = { viewModel.updateReadingMode(ReadingMode.ORIGINAL_LAYOUT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (settings.readingMode == ReadingMode.SMART_REFLOW) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Font Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.updateFontSize(settings.fontSize - 2) },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "${settings.fontSize} sp (Auto-wrapping)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AmberPrimary)
                        )
                        IconButton(
                            onClick = { viewModel.updateFontSize(settings.fontSize + 2) },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = if (settings.bionicReading) AmberPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = if (settings.bionicReading) BorderStroke(1.5.dp, AmberPrimary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateBionicReading(!settings.bionicReading) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡ Bionic Reading Mode",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.bionicReading) AmberPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Bolds initial letters to anchor eye fixation and double reading speed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = settings.bionicReading,
                                onCheckedChange = { viewModel.updateBionicReading(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmberPrimary,
                                    checkedTrackColor = AmberPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Theme Section
                Text(text = "Color Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeOptionCard(
                        title = "Light",
                        icon = Icons.Default.LightMode,
                        bg = LightPageBg,
                        textColor = Color.Black,
                        selected = settings.theme == ReaderTheme.LIGHT,
                        onClick = { viewModel.updateTheme(ReaderTheme.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Sepia",
                        icon = Icons.Default.MenuBook,
                        bg = SepiaPageBg,
                        textColor = SepiaPageText,
                        selected = settings.theme == ReaderTheme.SEPIA,
                        onClick = { viewModel.updateTheme(ReaderTheme.SEPIA) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        title = "Night",
                        icon = Icons.Default.DarkMode,
                        bg = NightPageBg,
                        textColor = Color(0xFFE0D8C8),
                        selected = settings.theme == ReaderTheme.NIGHT,
                        onClick = { viewModel.updateTheme(ReaderTheme.NIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Page Transition Mode
                Text(text = "Page Transition Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TransitionStyleCard(
                        title = "Realistic Page Flip",
                        subtitle = "Horizontal swiping",
                        selected = settings.transitionStyle == TransitionStyle.HORIZONTAL_FLIP,
                        onClick = { viewModel.updateTransitionStyle(TransitionStyle.HORIZONTAL_FLIP) },
                        modifier = Modifier.weight(1f)
                    )
                    TransitionStyleCard(
                        title = "Continuous Scroll",
                        subtitle = "Vertical list view",
                        selected = settings.transitionStyle == TransitionStyle.VERTICAL_SCROLL,
                        onClick = { viewModel.updateTransitionStyle(TransitionStyle.VERTICAL_SCROLL) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Margin Auto-Crop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto-Crop PDF Margins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Zoom in slightly to remove white document borders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Switch(
                        checked = settings.cropMargins,
                        onCheckedChange = { viewModel.updateCropMargins(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AmberPrimary, checkedTrackColor = AmberPrimary.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

private fun chunkTextIntoPages(text: String, linesPerPage: Int, charsPerLine: Int): List<String> {
    if (text.isEmpty()) return listOf("")
    
    val pages = mutableListOf<String>()
    val paragraphs = text.split("\n")
    
    var currentPageText = StringBuilder()
    var currentLinesUsed = 0.0
    var currentLineChars = 0
    
    for (para in paragraphs) {
        val trimmedPara = para.trim()
        if (trimmedPara.isEmpty()) {
            if (currentPageText.isNotEmpty() && currentLinesUsed + 1.0 <= linesPerPage) {
                currentPageText.append("\n\n")
                currentLinesUsed += 1.0
                currentLineChars = 0
            }
            continue
        }
        
        val words = trimmedPara.split(Regex("\\s+"))
        for (word in words) {
            val wordLen = word.length
            val neededChars = if (currentLineChars == 0) wordLen else currentLineChars + 1 + wordLen
            
            if (neededChars > charsPerLine && currentLineChars > 0) {
                // Move to next line
                currentLinesUsed += 1.0
                currentLineChars = 0
                
                // If page is full, push to next page!
                if (currentLinesUsed >= linesPerPage && currentPageText.isNotEmpty()) {
                    pages.add(currentPageText.toString().trim())
                    currentPageText = StringBuilder()
                    currentLinesUsed = 0.0
                } else if (currentPageText.isNotEmpty()) {
                    currentPageText.append("\n")
                }
            } else if (currentLineChars > 0) {
                currentPageText.append(" ")
                currentLineChars += 1
            }
            
            if (currentPageText.isEmpty() && currentLinesUsed >= linesPerPage) {
                currentLinesUsed = 0.0
            }
            
            currentPageText.append(word)
            
            if (currentLineChars == 0) {
                currentLineChars = wordLen
            } else {
                currentLineChars += wordLen - 1
            }
            
            while (currentLineChars >= charsPerLine) {
                currentLinesUsed += 1.0
                currentLineChars -= charsPerLine
                if (currentLinesUsed >= linesPerPage && currentPageText.isNotEmpty()) {
                    pages.add(currentPageText.toString().trim())
                    currentPageText = StringBuilder()
                    currentLinesUsed = 0.0
                    currentLineChars = 0
                }
            }
        }
        
        if (currentLineChars > 0) {
            currentLinesUsed += 1.0
            currentLineChars = 0
        }
        if (currentPageText.isNotEmpty() && currentLinesUsed + 1.0 <= linesPerPage) {
            currentPageText.append("\n\n")
            currentLinesUsed += 0.8
        }
    }
    
    if (currentPageText.isNotEmpty()) {
        val remaining = currentPageText.toString().trim()
        if (remaining.isNotEmpty()) {
            pages.add(remaining)
        }
    }
    
    return if (pages.isEmpty()) listOf(text) else pages
}

@Composable
fun ReaderPageContent(
    pageIndex: Int,
    settings: com.example.data.model.ReaderSettings,
    widthPx: Int,
    heightPx: Int,
    colorFilter: ColorFilter?,
    viewModel: ReaderViewModel,
    isMovingBackwards: Boolean = false,
    onNextPdfPage: () -> Unit = {},
    onPrevPdfPage: () -> Unit = {},
    onToggleOverlay: () -> Unit = {},
    onSubPageChanged: (Int, Int) -> Unit = { _, _ -> }
) {
    if (settings.readingMode == ReadingMode.SMART_REFLOW) {
        ReflowPageItem(
            pageIndex = pageIndex,
            settings = settings,
            colorFilter = colorFilter,
            viewModel = viewModel,
            widthPx = widthPx,
            heightPx = heightPx,
            isMovingBackwards = isMovingBackwards,
            onNextPdfPage = onNextPdfPage,
            onPrevPdfPage = onPrevPdfPage,
            onToggleOverlay = onToggleOverlay,
            onSubPageChanged = onSubPageChanged
        )
    } else {
        PdfPageItem(
            pageIndex = pageIndex,
            widthPx = widthPx,
            heightPx = heightPx,
            colorFilter = colorFilter,
            cropMargins = settings.cropMargins,
            viewModel = viewModel,
            isMovingBackwards = isMovingBackwards,
            onNextPdfPage = onNextPdfPage,
            onPrevPdfPage = onPrevPdfPage,
            onToggleOverlay = onToggleOverlay,
            onSubPageChanged = onSubPageChanged
        )
    }
}

@Composable
fun ReflowPageItem(
    pageIndex: Int,
    settings: com.example.data.model.ReaderSettings,
    colorFilter: ColorFilter?,
    viewModel: ReaderViewModel,
    widthPx: Int,
    heightPx: Int,
    isMovingBackwards: Boolean,
    onNextPdfPage: () -> Unit,
    onPrevPdfPage: () -> Unit,
    onToggleOverlay: () -> Unit,
    onSubPageChanged: (Int, Int) -> Unit
) {
    val isBookLoading by viewModel.isBookLoading.collectAsStateWithLifecycle()
    val contentState = produceState<com.example.pdf.ReflowPageContent?>(initialValue = null, pageIndex, isBookLoading) {
        if (!isBookLoading) {
            value = viewModel.getReflowContent(pageIndex)
        }
    }
    val content = contentState.value

    if (content == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AmberPrimary, modifier = Modifier.size(36.dp))
        }
    } else if (content.images.isEmpty() && content.text.trim().length < 350) {
        // Scanned page, diagram, or complex layout without selectable text -> Fallback to original image!
        PdfPageItem(
            pageIndex = pageIndex,
            widthPx = widthPx,
            heightPx = heightPx,
            colorFilter = colorFilter,
            cropMargins = true,
            viewModel = viewModel,
            isMovingBackwards = isMovingBackwards,
            onNextPdfPage = onNextPdfPage,
            onPrevPdfPage = onPrevPdfPage,
            onToggleOverlay = onToggleOverlay,
            onSubPageChanged = onSubPageChanged
        )
    } else {
        // Smart Text Reflow & Inline Images Reading Mode!
        val textColor = when (settings.theme) {
            com.example.data.model.ReaderTheme.LIGHT -> Color.Black
            com.example.data.model.ReaderTheme.SEPIA -> SepiaPageText
            com.example.data.model.ReaderTheme.NIGHT -> Color(0xFFE0D8C8)
        }
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp

        val linesPerPage = remember(settings.fontSize, settings.lineHeight, screenHeightDp) {
            ((screenHeightDp - 140) / (settings.fontSize * settings.lineHeight)).toInt().coerceAtLeast(4)
        }
        val charsPerLine = remember(settings.fontSize, screenWidthDp) {
            ((screenWidthDp - 44) / (settings.fontSize * 0.53f)).toInt().coerceAtLeast(10)
        }

        val chunks = remember(content.text, settings.fontSize, linesPerPage, charsPerLine) {
            chunkTextIntoPages(content.text, linesPerPage, charsPerLine)
        }

        val initialSubPage = if (isMovingBackwards && chunks.size > 1) chunks.size - 1 else 0
        val subPagerState = rememberPagerState(initialPage = initialSubPage) { chunks.size }

        var lastChunkCount by remember { mutableIntStateOf(chunks.size) }
        LaunchedEffect(chunks.size) {
            if (lastChunkCount != chunks.size && chunks.isNotEmpty()) {
                val approxRatio = subPagerState.currentPage.toFloat() / lastChunkCount.coerceAtLeast(1)
                val newPage = (approxRatio * chunks.size).toInt().coerceIn(0, chunks.size - 1)
                if (newPage != subPagerState.currentPage) {
                    subPagerState.scrollToPage(newPage)
                }
                lastChunkCount = chunks.size
            }
        }

        LaunchedEffect(subPagerState.currentPage, chunks.size) {
            onSubPageChanged(subPagerState.currentPage, chunks.size)
        }

        val scope = rememberCoroutineScope()
        val currentFontSize by rememberUpdatedState(settings.fontSize)
        var accumulatedFontSize by remember { mutableFloatStateOf(settings.fontSize.toFloat()) }
        LaunchedEffect(settings.fontSize) {
            if (kotlin.math.abs(accumulatedFontSize - settings.fontSize) >= 1f) {
                accumulatedFontSize = settings.fontSize.toFloat()
            }
        }

        HorizontalPager(
            state = subPagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1.0f) {
                            accumulatedFontSize = (accumulatedFontSize * zoom).coerceIn(12f, 38f)
                            val newSize = kotlin.math.round(accumulatedFontSize).toInt()
                            if (newSize != currentFontSize) {
                                viewModel.updateFontSize(newSize)
                            }
                        }
                    }
                }
                .pointerInput(subPagerState.currentPage, chunks.size) {
                    detectTapGestures { offset ->
                        val width = size.width
                        when {
                            offset.x < width * 0.22f -> {
                                if (subPagerState.currentPage > 0) {
                                    scope.launch { subPagerState.animateScrollToPage(subPagerState.currentPage - 1) }
                                } else {
                                    onPrevPdfPage()
                                }
                            }
                            offset.x > width * 0.78f -> {
                                if (subPagerState.currentPage < chunks.size - 1) {
                                    scope.launch { subPagerState.animateScrollToPage(subPagerState.currentPage + 1) }
                                } else {
                                    onNextPdfPage()
                                }
                            }
                            else -> {
                                onToggleOverlay()
                            }
                        }
                    }
                }
        ) { subIndex ->
            val chunkText = chunks.getOrNull(subIndex) ?: ""
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Render embedded PDF images/illustrations cleanly formatted inline!
                if (subIndex == 0 && content.images.isNotEmpty()) {
                    content.images.forEachIndexed { index, bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Page Illustration ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            colorFilter = colorFilter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                if (chunkText.isNotEmpty()) {
                    if (settings.bionicReading) {
                        BionicText(
                            text = chunkText,
                            fontSize = settings.fontSize,
                            lineHeight = settings.fontSize * settings.lineHeight,
                            color = textColor
                        )
                    } else {
                        Text(
                            text = chunkText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = settings.fontSize.sp,
                                lineHeight = (settings.fontSize * settings.lineHeight).sp,
                                color = textColor,
                                fontWeight = FontWeight.Normal
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageItem(
    pageIndex: Int,
    widthPx: Int,
    heightPx: Int,
    colorFilter: ColorFilter?,
    cropMargins: Boolean,
    viewModel: ReaderViewModel,
    isMovingBackwards: Boolean = false,
    onNextPdfPage: () -> Unit = {},
    onPrevPdfPage: () -> Unit = {},
    onToggleOverlay: () -> Unit = {},
    onSubPageChanged: (Int, Int) -> Unit = { _, _ -> }
) {
    // Zoom & Pan state for original image mode
    var scale by remember { mutableFloatStateOf(if (cropMargins) 1.0f else 1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val isBookLoading by viewModel.isBookLoading.collectAsStateWithLifecycle()
    val bitmapState = produceState<Bitmap?>(initialValue = null, pageIndex, widthPx, heightPx, isBookLoading) {
        if (!isBookLoading) {
            value = viewModel.getPageBitmap(pageIndex, widthPx, heightPx)
        }
    }

    LaunchedEffect(pageIndex) {
        onSubPageChanged(0, 1)
    }

    val bitmap = bitmapState.value
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1.0f, 4.0f)
                    if (scale > 1.0f) {
                        offset += pan
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = if (cropMargins) ContentScale.FillWidth else ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else {
            CircularProgressIndicator(color = AmberPrimary, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    textColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, AmberPrimary) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 1.dp),
        modifier = modifier
            .height(85.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = textColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun TransitionStyleCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AmberPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, AmberPrimary) else null,
        modifier = modifier
            .height(75.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selected) AmberPrimary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                if (selected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AudiobookPlayerCard(
    viewModel: com.example.ui.viewmodel.ReaderViewModel,
    onClose: () -> Unit
) {
    val isPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()
    val currentSentence by viewModel.currentTtsSentence.collectAsStateWithLifecycle()
    val progress by viewModel.ttsProgress.collectAsStateWithLifecycle()
    val speed by viewModel.ttsSpeed.collectAsStateWithLifecycle()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headset,
                        contentDescription = "Audiobook Mode",
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Audiobook Mode",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress.second > 0) {
                        Text(
                            text = "${progress.first}/${progress.second}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Audiobook",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Currently Reading Text Banner
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (currentSentence.isNotEmpty()) currentSentence else "Initializing offline voice engine...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Selector Button
                TextButton(
                    onClick = {
                        val nextSpeed = when (speed) {
                            0.75f -> 1.0f
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 0.75f
                        }
                        viewModel.setTtsSpeed(nextSpeed)
                    }
                ) {
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AmberPrimary
                    )
                }

                // Previous Sentence
                IconButton(onClick = { viewModel.skipTtsPreviousSentence() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Sentence",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play / Pause Button
                FloatingActionButton(
                    onClick = { viewModel.toggleTts() },
                    containerColor = AmberPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                // Next Sentence
                IconButton(onClick = { viewModel.skipTtsNextSentence() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Sentence",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Stop Button
                IconButton(onClick = { viewModel.stopTts() }) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun BionicText(
    text: String,
    fontSize: Int,
    lineHeight: Float,
    color: Color
) {
    val annotatedString = remember(text, color) {
        buildAnnotatedString {
            val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
            for (word in words) {
                if (word.isBlank()) {
                    append(word)
                } else {
                    val len = word.length
                    val boldLen = when {
                        len <= 3 -> 1
                        len <= 5 -> 2
                        len <= 8 -> 3
                        else -> len / 2
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = color)) {
                        append(word.take(boldLen))
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = color.copy(alpha = 0.85f))) {
                        append(word.drop(boldLen))
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.sp,
            lineHeight = lineHeight.sp
        ),
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun RsvpPlayerCard(
    viewModel: com.example.ui.viewmodel.ReaderViewModel,
    onClose: () -> Unit
) {
    val isPlaying by viewModel.isRsvpPlaying.collectAsStateWithLifecycle()
    val words by viewModel.rsvpWords.collectAsStateWithLifecycle()
    val currentIndex by viewModel.rsvpCurrentWordIndex.collectAsStateWithLifecycle()
    val wpm by viewModel.rsvpWpm.collectAsStateWithLifecycle()

    val currentWord = if (words.isNotEmpty() && currentIndex in words.indices) {
        words[currentIndex]
    } else {
        "Ready..."
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed Reading Mode",
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RSVP Speed Reading",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (words.isNotEmpty()) {
                        Text(
                            text = "${currentIndex + 1}/${words.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close RSVP",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Focal Word Display Box (RSVP Window)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, AmberPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Focal indicators (subtle top/bottom notches)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(width = 2.dp, height = 6.dp).background(AmberPrimary))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(width = 2.dp, height = 6.dp).background(AmberPrimary))
                    }

                    // Render current word with Bionic/ORP highlighting
                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    val annotatedWord = remember(currentWord, onSurfaceColor) {
                        buildAnnotatedString {
                            val len = currentWord.length
                            val pivot = when {
                                len <= 1 -> 1
                                len <= 5 -> 2
                                len <= 9 -> 3
                                else -> 4
                            }.coerceAtMost(len)

                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = AmberPrimary, fontSize = 28.sp)) {
                                append(currentWord.take(pivot))
                            }
                            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = onSurfaceColor, fontSize = 28.sp)) {
                                append(currentWord.drop(pivot))
                            }
                        }
                    }
                    Text(
                        text = annotatedWord,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // WPM Selector & Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WPM Speed Selector
                TextButton(
                    onClick = {
                        val nextWpm = when (wpm) {
                            200 -> 300
                            300 -> 400
                            400 -> 500
                            500 -> 600
                            600 -> 800
                            else -> 200
                        }
                        viewModel.setRsvpWpm(nextWpm)
                    }
                ) {
                    Text(
                        text = "${wpm} WPM",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AmberPrimary
                    )
                }

                // Skip Back 10 words
                IconButton(onClick = { viewModel.skipRsvpPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10 words",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play / Pause Button
                FloatingActionButton(
                    onClick = { viewModel.toggleRsvp() },
                    containerColor = AmberPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

                // Skip Forward 10 words
                IconButton(onClick = { viewModel.skipRsvpNext() }) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10 words",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Stop Button
                IconButton(onClick = { viewModel.stopRsvp() }) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
