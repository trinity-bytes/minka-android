package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the Material 3 role mapping documented on the Figma Foundations page (Color panels). */
class ColorSchemeTest {

    @Test
    fun `light scheme maps the Figma Light roles`() {
        with(LightColorScheme) {
            assertEquals(Color(0xFF1A7A43), primary)
            assertEquals(Color(0xFF2ECC71), primaryContainer)
            assertEquals(Color(0xFF0B0D0C), onPrimaryContainer)
            assertEquals(Color(0xFF5B3FA8), secondary)
            assertEquals(Color(0xFFC4B2F5), secondaryContainer)
            assertEquals(Color(0xFF7A5C00), tertiary)
            assertEquals(Color(0xFFFCEFC7), tertiaryContainer)
            assertEquals(Color(0xFFC0392B), error)
            assertEquals(Color(0xFFEEF3EF), surface)
            assertEquals(Color(0xFFFFFFFF), surfaceContainerLowest)
            assertEquals(Color(0xFFDFE8E2), surfaceContainerHighest)
            assertEquals(Color(0xFF10291C), onSurface)
            assertEquals(Color(0xFF6FDC9C), inversePrimary)
            assertEquals(Color(0xFFD5E0D8), outlineVariant)
            assertEquals(Color(0xFF0A1D13), scrim)
        }
    }

    @Test
    fun `dark scheme maps the Figma Dark roles`() {
        with(DarkColorScheme) {
            assertEquals(Color(0xFF6FDC9C), primary)
            assertEquals(Color(0xFF0E4A28), onPrimary)
            assertEquals(Color(0xFF146236), primaryContainer)
            assertEquals(Color(0xFFC4B2F5), secondary)
            assertEquals(Color(0xFFF0C64A), tertiary)
            assertEquals(Color(0xFFFFB4A9), error)
            assertEquals(Color(0xFF7D2419), errorContainer)
            assertEquals(Color(0xFF0A1D13), surface)
            assertEquals(Color(0xFF06140D), surfaceContainerLowest)
            assertEquals(Color(0xFF2C4134), surfaceContainerHighest)
            assertEquals(Color(0xFFE6EEE8), onSurface)
            assertEquals(Color(0xFFE6EEE8), inverseSurface)
            assertEquals(Color(0xFF3A4B40), outlineVariant)
            assertEquals(Color(0xFF000000), scrim)
        }
    }

    @Test
    fun `extended colors keep the brand fixed and swap the accent containers in dark`() {
        assertEquals(Color(0xFF2ECC71), LightMinkaColors.brand)
        assertEquals(LightMinkaColors.brand, DarkMinkaColors.brand)
        assertEquals(Color(0xFFDCECF9), LightMinkaColors.skyContainer)
        assertEquals(Color(0xFF174A6E), DarkMinkaColors.skyContainer)
        assertEquals(Color(0xFFFFFFFF), LightMinkaColors.raised)
        assertEquals(Color(0xFF172F22), DarkMinkaColors.raised)
        assertEquals(Color(0xFF213428), DarkMinkaColors.overlay)
    }
}
