package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Pins the Material 3 role mapping documented on the Figma Foundations page (Color panels). */
class ColorSchemeTest {

    @Test
    fun `theme does not expose a dark mode switch`() {
        val theme = Class.forName("com.trinitybytes.minka.core.designsystem.theme.ThemeKt")
        val methods = theme.declaredMethods.filter { it.name == "MinkaTheme" }
        assertFalse(methods.isEmpty())
        assertFalse(methods.any { method -> method.parameterTypes.contains(Boolean::class.javaPrimitiveType) })
    }

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
    fun `extended colors retain the light brand and accents`() {
        assertEquals(Color(0xFF2ECC71), LightMinkaColors.brand)
        assertEquals(Color(0xFFDCECF9), LightMinkaColors.skyContainer)
        assertEquals(Color(0xFFFFFFFF), LightMinkaColors.raised)
        assertEquals(Color(0xFFFFFFFF), LightMinkaColors.overlay)
        assertEquals(Color(0xFFD7F5E3), LightMinkaColors.mint)
    }
}
