package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Minka theme: Material 3 roles plus [MinkaTheme] tokens for what Material has no slot for.
 * No dynamic color: the brand palette is part of the identity.
 */
@Composable
fun MinkaTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMinkaColors provides LightMinkaColors,
    ) {
        MaterialExpressiveTheme(
            colorScheme = LightColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = MinkaTypography,
            shapes = MinkaMaterialShapes,
            content = content,
        )
    }
}

object MinkaTheme {
    val colors: MinkaColors
        @Composable @ReadOnlyComposable
        get() = LocalMinkaColors.current

    val typography: MinkaTextStyles get() = MinkaTextStyleTokens
}
