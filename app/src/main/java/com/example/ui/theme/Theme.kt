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
    primary = AmberPrimary,
    secondary = AccentOrange,
    tertiary = ProgressGreen,
    background = NightPageBg,
    surface = NightCardBg,
    onPrimary = Color.White,
    onBackground = NightPageText,
    onSurface = NightPageText,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AmberPrimary,
    secondary = AmberSecondary,
    tertiary = ProgressGreen,
    background = LightPageBg,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = LightPageText,
    onSurface = LightPageText,
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
