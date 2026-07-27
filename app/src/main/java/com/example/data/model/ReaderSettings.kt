package com.example.data.model

enum class ReaderTheme {
    LIGHT,
    SEPIA,
    NIGHT
}

enum class TransitionStyle {
    HORIZONTAL_FLIP,
    VERTICAL_SCROLL
}

enum class ReadingMode {
    SMART_REFLOW,
    ORIGINAL_LAYOUT
}

data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val transitionStyle: TransitionStyle = TransitionStyle.HORIZONTAL_FLIP,
    val brightness: Float = -1f, // -1f means system brightness, 0f..1f is custom brightness
    val cropMargins: Boolean = true,
    val zoomScale: Float = 1.0f,
    val readingMode: ReadingMode = ReadingMode.ORIGINAL_LAYOUT,
    val fontSize: Int = 18,
    val lineHeight: Float = 1.5f
)
