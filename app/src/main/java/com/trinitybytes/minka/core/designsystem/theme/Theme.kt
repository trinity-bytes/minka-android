package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Minka theme: Material 3 roles plus [MinkaTheme.colors] for the colors Material has no role for.
 * No dynamic color: the brand palette is part of the identity.
 */
@Composable
fun MinkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMinkaColors provides if (darkTheme) DarkMinkaColors else LightMinkaColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography(),
            content = content,
        )
    }
}

object MinkaTheme {
    val colors: MinkaColors
        @Composable @ReadOnlyComposable
        get() = LocalMinkaColors.current
}
