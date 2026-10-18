package de.dirkgerhardt.bahnfrei.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Material-3-Farbschema mit Blau als Grundton (statt des Standard-Lilas)
private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF1F4E79),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4F2),
    onPrimaryContainer = Color(0xFF0B2540),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF00677F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB8EAFF),
    onTertiaryContainer = Color(0xFF001F28),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceTint = Color(0xFF1F4E79),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC2C7CF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FF),
    surfaceContainer = Color(0xFFF1F4FA),
    surfaceContainerHigh = Color(0xFFEBEEF4),
    surfaceContainerHighest = Color(0xFFE5E8EE)
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9C8E8),
    onPrimary = Color(0xFF0B2540),
    primaryContainer = Color(0xFF1F4E79),
    onPrimaryContainer = Color(0xFFD6E4F2),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFF5FD4F8),
    onTertiary = Color(0xFF003543),
    tertiaryContainer = Color(0xFF004E5F),
    onTertiaryContainer = Color(0xFFB8EAFF),
    background = Color(0xFF25282D),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF25282D),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    surfaceTint = Color(0xFFA9C8E8),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    surfaceContainerLowest = Color(0xFF1A1D20),
    surfaceContainerLow = Color(0xFF2B2F34),
    surfaceContainer = Color(0xFF2F3338),
    surfaceContainerHigh = Color(0xFF383C42),
    surfaceContainerHighest = Color(0xFF42464C)
)

/**
 * Im Dunkelmodus sind Karten (surfaceContainerLow/Container) im M3-Standard kaum vom
 * Hintergrund unterscheidbar. Hier wird ein klarer Abstand erzeugt, indem die
 * Kartenflächen auf die helleren Container-Stufen des jeweiligen Schemas gesetzt werden.
 */
private fun ColorScheme.withVisibleCards(): ColorScheme = copy(
    surfaceContainerLow = surfaceContainerHigh,
    surfaceContainer = surfaceContainerHighest
)

@Composable
fun BahnFreiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context).withVisibleCards()
            else dynamicLightColorScheme(context)
        }

        darkTheme -> BlueDarkColorScheme.withVisibleCards()
        else -> BlueLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}











