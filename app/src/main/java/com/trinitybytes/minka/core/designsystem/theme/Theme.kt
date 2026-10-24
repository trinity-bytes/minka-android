package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = Mint,
    onPrimaryContainer = Ink,
    secondary = LavenderInk,
    onSecondary = OnPrimary,
    secondaryContainer = LavenderSoft,
    onSecondaryContainer = OnLavender,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = Ink2,
    surfaceContainerLowest = Card,
    outline = Gray400,
    outlineVariant = Gray100,
    error = Error,
    onError = OnPrimary,
)

/**
 * Light only for now: the dark scheme and the Fraunces/Inter type scale come from Figma in phase 1.
 */
@Composable
fun MinkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content,
    )
}
