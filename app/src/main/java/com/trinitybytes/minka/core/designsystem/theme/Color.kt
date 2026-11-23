package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Figma Foundations > Color panel (Light). Brand green #2ECC71 is primaryContainer:
// fill only, never text. Lavender is secondary and amber is tertiary (points, achievements).

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A7A43),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2ECC71),
    onPrimaryContainer = Color(0xFF0B0D0C),
    secondary = Color(0xFF5B3FA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC4B2F5),
    onSecondaryContainer = Color(0xFF2A1858),
    tertiary = Color(0xFF7A5C00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCEFC7),
    onTertiaryContainer = Color(0xFF7A5C00),
    error = Color(0xFFC0392B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE3E0),
    onErrorContainer = Color(0xFFA93226),
    background = Color(0xFFEEF3EF),
    onBackground = Color(0xFF10291C),
    surface = Color(0xFFEEF3EF),
    onSurface = Color(0xFF10291C),
    surfaceVariant = Color(0xFFDFE8E2),
    onSurfaceVariant = Color(0xFF3D5547),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F8F5),
    surfaceContainer = Color(0xFFEEF3EF),
    surfaceContainerHigh = Color(0xFFE6EEE8),
    surfaceContainerHighest = Color(0xFFDFE8E2),
    inverseSurface = Color(0xFF10291C),
    inverseOnSurface = Color(0xFFEEF3EF),
    inversePrimary = Color(0xFF6FDC9C),
    outline = Color(0xFF7B8A80),
    outlineVariant = Color(0xFFD5E0D8),
    scrim = Color(0xFF0A1D13),
)

/** Minka colors with no Material 3 role ("Mink'a extras" and "Accent" on the Figma Color panels). */
@Immutable
data class MinkaColors(
    val brand: Color,
    val onBrand: Color,
    val info: Color,
    val amber: Color,
    val onAmber: Color,
    val sky: Color,
    val onSky: Color,
    val skyContainer: Color,
    val onSkyContainer: Color,
    val coral: Color,
    val onCoral: Color,
    val coralContainer: Color,
    val onCoralContainer: Color,
    val lavenderContainer: Color,
    /** Figma `brand/mint`: soft green stage behind brand content. */
    val mint: Color,
    /** Cards that sit above the page. */
    val raised: Color,
    /** Sheets, menus and dialogs. */
    val overlay: Color,
)

internal val LightMinkaColors = MinkaColors(
    brand = Color(0xFF2ECC71),
    onBrand = Color(0xFF0B0D0C),
    info = Color(0xFF2874A6),
    amber = Color(0xFFF0C64A),
    onAmber = Color(0xFF3D2E00),
    sky = Color(0xFF9CCBEF),
    onSky = Color(0xFF174A6E),
    skyContainer = Color(0xFFDCECF9),
    onSkyContainer = Color(0xFF174A6E),
    coral = Color(0xFFFFB4A9),
    onCoral = Color(0xFF5C1A12),
    coralContainer = Color(0xFFFBE3E0),
    onCoralContainer = Color(0xFF5C1A12),
    lavenderContainer = Color(0xFFECE6FD),
    mint = Color(0xFFD7F5E3),
    raised = Color(0xFFFFFFFF),
    overlay = Color(0xFFFFFFFF),
)

internal val LocalMinkaColors = staticCompositionLocalOf { LightMinkaColors }
