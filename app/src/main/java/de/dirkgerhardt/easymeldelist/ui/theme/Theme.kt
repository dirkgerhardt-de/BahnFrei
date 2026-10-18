package de.dirkgerhardt.easymeldelist.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = OceanSandColors.darkPrimary,
    onPrimary = OceanSandColors.darkOnPrimary,
    primaryContainer = OceanSandColors.darkPrimaryContainer,
    onPrimaryContainer = OceanSandColors.darkOnPrimaryContainer,
    secondary = OceanSandColors.darkSecondary,
    onSecondary = Color(0xFF3A2E17),
    secondaryContainer = OceanSandColors.darkSecondaryContainer,
    onSecondaryContainer = OceanSandColors.darkOnSecondaryContainer,
    tertiary = OceanSandColors.darkTertiary,
    background = OceanSandColors.darkBackground,
    onBackground = OceanSandColors.darkOnBackground,
    surface = OceanSandColors.darkSurface,
    onSurface = OceanSandColors.darkOnSurface,
    surfaceVariant = OceanSandColors.darkSurfaceVariant,
    onSurfaceVariant = OceanSandColors.darkOnSurfaceVariant,
    surfaceContainerLowest = OceanSandColors.darkSurfaceContainerLowest,
    surfaceContainerLow = OceanSandColors.darkSurfaceContainerLow,
    surfaceContainer = OceanSandColors.darkSurfaceContainer,
    surfaceContainerHigh = OceanSandColors.darkSurfaceContainerHigh,
    surfaceContainerHighest = OceanSandColors.darkSurfaceContainerHighest
)

private val LightColorScheme = lightColorScheme(
    primary = OceanSandColors.lightPrimary,
    onPrimary = OceanSandColors.lightOnPrimary,
    primaryContainer = OceanSandColors.lightPrimaryContainer,
    onPrimaryContainer = OceanSandColors.lightOnPrimaryContainer,
    secondary = OceanSandColors.lightSecondary,
    onSecondary = Color.White,
    secondaryContainer = OceanSandColors.lightSecondaryContainer,
    onSecondaryContainer = OceanSandColors.lightOnSecondaryContainer,
    tertiary = OceanSandColors.lightTertiary,
    background = OceanSandColors.lightBackground,
    onBackground = OceanSandColors.lightOnBackground,
    surface = OceanSandColors.lightSurface,
    onSurface = OceanSandColors.lightOnSurface,
    surfaceVariant = OceanSandColors.lightSurfaceVariant,
    onSurfaceVariant = OceanSandColors.lightOnSurfaceVariant,
    surfaceContainerLowest = OceanSandColors.lightSurfaceContainerLowest,
    surfaceContainerLow = OceanSandColors.lightSurfaceContainerLow,
    surfaceContainer = OceanSandColors.lightSurfaceContainer,
    surfaceContainerHigh = OceanSandColors.lightSurfaceContainerHigh,
    surfaceContainerHighest = OceanSandColors.lightSurfaceContainerHighest
)

private fun ColorScheme.withNeutralSurfaces(dark: Boolean): ColorScheme =
    if (dark) copy(
        background = OceanSandColors.darkBackground,
        onBackground = OceanSandColors.darkOnBackground,
        surface = OceanSandColors.darkSurface,
        onSurface = OceanSandColors.darkOnSurface,
        surfaceVariant = OceanSandColors.darkSurfaceVariant,
        onSurfaceVariant = OceanSandColors.darkOnSurfaceVariant,
        surfaceContainerLowest = OceanSandColors.darkSurfaceContainerLowest,
        surfaceContainerLow = OceanSandColors.darkSurfaceContainerLow,
        surfaceContainer = OceanSandColors.darkSurfaceContainer,
        surfaceContainerHigh = OceanSandColors.darkSurfaceContainerHigh,
        surfaceContainerHighest = OceanSandColors.darkSurfaceContainerHighest
    ) else copy(
        background = OceanSandColors.lightBackground,
        onBackground = OceanSandColors.lightOnBackground,
        surface = OceanSandColors.lightSurface,
        onSurface = OceanSandColors.lightOnSurface,
        surfaceVariant = OceanSandColors.lightSurfaceVariant,
        onSurfaceVariant = OceanSandColors.lightOnSurfaceVariant,
        surfaceContainerLowest = OceanSandColors.lightSurfaceContainerLowest,
        surfaceContainerLow = OceanSandColors.lightSurfaceContainerLow,
        surfaceContainer = OceanSandColors.lightSurfaceContainer,
        surfaceContainerHigh = OceanSandColors.lightSurfaceContainerHigh,
        surfaceContainerHighest = OceanSandColors.lightSurfaceContainerHighest
    )

@Composable
fun EasyMeldelistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Akzentfarben aus dem System, Flächen/Text bleiben lesbar (eigene Neutraltöne)
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamic.withNeutralSurfaces(darkTheme)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}