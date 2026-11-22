package com.fhdvideobox.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.fhdvideobox.app.data.AppTheme

val FhdBlue = Color(0xFF1E88FF)
val FhdCyan = Color(0xFF19D3F7)
val FhdNavy = Color(0xFF0C1220)

private val DarkGreen = darkColorScheme(
    primary = Color(0xFF00B386),          // акцент как в эталонном приложении
    onPrimary = Color.White,
    secondary = Color(0xFF26A69A),
    background = Color(0xFF141414),
    surface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF3A3A3A),
)

private val DarkBlue = darkColorScheme(
    primary = FhdBlue,
    onPrimary = Color.White,
    secondary = FhdCyan,
    background = FhdNavy,
    surface = Color(0xFF141C2E),
    surfaceVariant = Color(0xFF1C2740),
    onBackground = Color(0xFFE8EEFA),
    onSurface = Color(0xFFE8EEFA),
    onSurfaceVariant = Color(0xFF9FB0CC),
    outline = Color(0xFF2B3955),
)

private val Black = darkColorScheme(
    primary = FhdCyan,
    onPrimary = Color.Black,
    secondary = FhdBlue,
    background = Color.Black,
    surface = Color(0xFF0E0E0E),
    surfaceVariant = Color(0xFF181818),
    onBackground = Color(0xFFE6E6E6),
    onSurface = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outline = Color(0xFF262626),
)

private val Light = lightColorScheme(
    primary = Color(0xFF0B6CDB),
    onPrimary = Color.White,
    secondary = Color(0xFF00A3C4),
    background = Color(0xFFF4F6FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EBF3),
    onBackground = Color(0xFF151A22),
    onSurface = Color(0xFF151A22),
    onSurfaceVariant = Color(0xFF55606F),
    outline = Color(0xFFD0D7E2),
)

@Composable
fun FhdTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val scheme = when (theme) {
        AppTheme.DARK_GREEN -> DarkGreen
        AppTheme.DARK_BLUE -> DarkBlue
        AppTheme.BLACK -> Black
        AppTheme.LIGHT -> Light
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
