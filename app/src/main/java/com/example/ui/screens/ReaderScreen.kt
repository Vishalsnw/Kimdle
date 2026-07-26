package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isOverlayVisible by viewModel.isOverlayVisible.collectAsStateWithLifecycle()
    val currentPageIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    BackHandler {
        if (isOverlayVisible) {
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
        if (book == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AmberPrimary)
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

            if (settings.transitionStyle == TransitionStyle.HORIZONTAL_FLIP) {
                // Horizontal Kindle Page Flip Mode
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val width = size.width
                                when {
                                    offset.x < width * 0.22f -> {
                                        // Tap left edge -> Previous page
                                        if (pagerState.currentPage > 0) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                        }
                                    }
                                    offset.x > width * 0.78f -> {
                                        // Tap right edge -> Next page
                                        if (pagerState.currentPage < totalPages - 1) {
                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                        }
                                    }
                                    else -> {
                                        // Tap center -> Toggle overlays
                                        viewModel.toggleOverlay()
                                    }
                                }
                            }
                        }
                ) { pageIndex ->
                    PdfPageItem(
                        pageIndex = pageIndex,
                        widthPx = screenWidthPx,
                        heightPx = screenHeightPx,
                        colorFilter = pdfColorFilter,
                        cropMargins = settings.cropMargins,
                        viewModel = viewModel
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
                            PdfPageItem(
                                pageIndex = pageIndex,
                                widthPx = screenWidthPx,
                                heightPx = screenHeightPx,
                                colorFilter = pdfColorFilter,
                                cropMargins = settings.cropMargins,
                                viewModel = viewModel
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

            // Bottom Navigation Overlay
            AnimatedVisibility(
                visible = isOverlayVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
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
                            Text(
                                text = "Page ${currentPageIndex + 1} of $totalPages",
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
                    text = "Kindle Reading Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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
                        title = "Kindle Page Flip",
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

@Composable
fun PdfPageItem(
    pageIndex: Int,
    widthPx: Int,
    heightPx: Int,
    colorFilter: ColorFilter?,
    cropMargins: Boolean,
    viewModel: ReaderViewModel
) {
    // Zoom state
    var scale by remember { mutableFloatStateOf(if (cropMargins) 1.08f else 1.0f) }
    LaunchedEffect(cropMargins) {
        scale = if (cropMargins) 1.08f else 1.0f
    }

    val bitmapState = produceState<Bitmap?>(initialValue = null, pageIndex, widthPx, heightPx) {
        value = viewModel.getPageBitmap(pageIndex, widthPx, heightPx)
    }

    val bitmap = bitmapState.value
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1.0f, 3.5f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
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
