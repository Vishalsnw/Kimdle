package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GoldPrimary,
    secondary = AccentOrange,
    tertiary = ProgressGreen,
    background = ObsidianDarkBg,
    surface = ObsidianSurface,
    surfaceVariant = ObsidianSurfaceVariant,
    outline = ObsidianBorder,
    onPrimary = Color.Black,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GoldSecondary,
    secondary = AccentOrange,
    tertiary = ProgressGreen,
    background = LightPageBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = Color(0xFFCBD5E1),
    onPrimary = Color.White,
    onBackground = LightPageText,
    onSurface = LightPageText,
    onSurfaceVariant = Color(0xFF64748B)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic color so our cozy amber/sepia palette is always preserved!
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
