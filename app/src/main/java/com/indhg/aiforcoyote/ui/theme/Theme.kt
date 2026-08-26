package com.indhg.aiforcoyote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold = Color(0xFFF7D97A)
val GoldBright = Color(0xFFFFE59A)
val Ink = Color(0xFF14100A)
val Ink2 = Color(0xFF1E1810)
val Ink3 = Color(0xFF2A2216)
val Line = Color(0xFF3A3126)
val Muted = Color(0xFFA8A090)
val Faint = Color(0xFF6E6658)
val TextMain = Color(0xFFEFE8DC)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    background = Color.Black,
    surface = Ink2,
    onSurface = TextMain,
    outline = Line,
)

@Composable
fun CoyoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
