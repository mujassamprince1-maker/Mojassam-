package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.viewmodel.ColorTheme

@Composable
fun PrivoraTheme(
    themeSelection: ColorTheme = ColorTheme.CYAN_NEON,
    content: @Composable () -> Unit
) {
    val (primary, secondary, primaryContainer) = when (themeSelection) {
        ColorTheme.CYAN_NEON -> Triple(CyanPrimary, CyanSecondary, CyanLight)
        ColorTheme.EMERALD_MATRIX -> Triple(EmeraldPrimary, EmeraldSecondary, EmeraldLight)
        ColorTheme.CRIMSON_AURA -> Triple(CrimsonPrimary, CrimsonSecondary, CrimsonLight)
        ColorTheme.AMETHYST_CYBER -> Triple(AmethystPrimary, AmethystSecondary, AmethystLight)
    }

    val scheme = darkColorScheme(
        primary = primary,
        primaryContainer = primaryContainer,
        secondary = primary,
        secondaryContainer = secondary,
        tertiary = primary,
        background = ObsidianDark,
        surface = AbyssBlack,
        surfaceVariant = SteelSlate,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = TextCrisp,
        onSurface = TextCrisp,
        onSurfaceVariant = TextGray,
        error = CyberRed,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
