package com.chiranth7.regibook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDarkTheme = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = NotebookButtonBgDark,
    onPrimary = NotebookPaperDark,
    background = NotebookPaperDark,
    onBackground = NotebookInkDark,
    surface = NotebookPaperSurfaceDark,
    onSurface = NotebookInkDark,
    surfaceVariant = NotebookActionPillDark,
    onSurfaceVariant = NotebookInkDarkSecondary,
    outline = NotebookRuledLineDark,
    outlineVariant = Color(0xFF374151),
    error = Color(0xFFF87171),
    onError = Color(0xFF121316)
)

private val LightColorScheme = lightColorScheme(
    primary = NotebookButtonBgLight,
    onPrimary = Color.White,
    background = Color(0xFFFFFFFF),       // Pure #FFFFFF as requested!
    onBackground = NotebookInkLight,
    surface = Color(0xFFFFFFFF),          // Pure #FFFFFF surface
    onSurface = NotebookInkLight,
    surfaceVariant = NotebookActionPillLight,
    onSurfaceVariant = NotebookInkSecondary,
    outline = NotebookRuledLineLight,
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun RegisterBookTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
