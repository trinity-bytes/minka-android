package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Figma Foundations > Color panels (Light/Dark). Brand green #2ECC71 is primaryContainer:
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

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6FDC9C),
    onPrimary = Color(0xFF0E4A28),
    primaryContainer = Color(0xFF146236),
    onPrimaryContainer = Color(0xFFD7F5E3),
    secondary = Color(0xFFC4B2F5),
    onSecondary = Color(0xFF2A1858),
    secondaryContainer = Color(0xFF3F2A7A),
    onSecondaryContainer = Color(0xFFECE6FD),
    tertiary = Color(0xFFF0C64A),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF5C4500),
    onTertiaryContainer = Color(0xFFFCEFC7),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF5C1A12),
    errorContainer = Color(0xFF7D2419),
    onErrorContainer = Color(0xFFFBE3E0),
    background = Color(0xFF0A1D13),
    onBackground = Color(0xFFE6EEE8),
    surface = Color(0xFF0A1D13),
    onSurface = Color(0xFFE6EEE8),
    surfaceVariant = Color(0xFF2C4134),
    onSurfaceVariant = Color(0xFFC2CFC6),
    surfaceContainerLowest = Color(0xFF06140D),
    surfaceContainerLow = Color(0xFF10291C),
    surfaceContainer = Color(0xFF172F22),
    surfaceContainerHigh = Color(0xFF213428),
    surfaceContainerHighest = Color(0xFF2C4134),
    inverseSurface = Color(0xFFE6EEE8),
    inverseOnSurface = Color(0xFF10291C),
    inversePrimary = Color(0xFF1A7A43),
    outline = Color(0xFF95A399),
    outlineVariant = Color(0xFF3A4B40),
    scrim = Color(0xFF000000),
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
    raised = Color(0xFFFFFFFF),
    overlay = Color(0xFFFFFFFF),
)

internal val DarkMinkaColors = MinkaColors(
    brand = Color(0xFF2ECC71),
    onBrand = Color(0xFF0B0D0C),
    info = Color(0xFF9CCBEF),
    amber = Color(0xFFF0C64A),
    onAmber = Color(0xFF3D2E00),
    sky = Color(0xFF9CCBEF),
    onSky = Color(0xFF174A6E),
    skyContainer = Color(0xFF174A6E),
    onSkyContainer = Color(0xFFDCECF9),
    coral = Color(0xFFFFB4A9),
    onCoral = Color(0xFF5C1A12),
    coralContainer = Color(0xFF5C1A12),
    onCoralContainer = Color(0xFFFBE3E0),
    lavenderContainer = Color(0xFF3F2A7A),
    raised = Color(0xFF172F22),
    overlay = Color(0xFF213428),
)

internal val LocalMinkaColors = staticCompositionLocalOf { LightMinkaColors }
