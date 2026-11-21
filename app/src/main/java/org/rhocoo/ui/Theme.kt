package org.rhocoo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF6A1BC2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDCFF),
    onPrimaryContainer = Color(0xFF270058),
    secondaryContainer = Color(0xFFE9DEF8),
    onSecondaryContainer = Color(0xFF221830),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFD5BAFF),
    onPrimary = Color(0xFF41008B),
    primaryContainer = Color(0xFF5A00BF),
    onPrimaryContainer = Color(0xFFEBDCFF),
    secondaryContainer = Color(0xFF4B3F5D),
    onSecondaryContainer = Color(0xFFE9DEF8),
)

@Composable
fun RhoCooTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
