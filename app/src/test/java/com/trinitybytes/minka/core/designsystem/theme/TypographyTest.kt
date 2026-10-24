package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Pins the type scale from the Figma Foundations page (Tipografía). */
class TypographyTest {

    private fun assertStyle(style: TextStyle, size: Int, lineHeight: Int, tracking: Float, weight: FontWeight) {
        assertEquals(size.sp, style.fontSize)
        assertEquals(lineHeight.sp, style.lineHeight)
        assertEquals(tracking.sp, style.letterSpacing)
        assertEquals(weight, style.fontWeight)
    }

    @Test
    fun `display and headline styles follow the Figma Fraunces scale`() {
        with(MinkaTypography) {
            assertStyle(displayLarge, 57, 64, -1.4f, FontWeight.Black)
            assertStyle(displayMedium, 45, 52, -1.1f, FontWeight.Black)
            assertStyle(displaySmall, 36, 44, -0.9f, FontWeight.Black)
            assertStyle(headlineLarge, 32, 40, -0.3f, FontWeight.Bold)
            assertStyle(headlineMedium, 28, 36, -0.2f, FontWeight.Bold)
            assertStyle(headlineSmall, 24, 32, 0f, FontWeight.Bold)
            assertStyle(titleLarge, 22, 28, 0f, FontWeight.Bold)
        }
    }

    @Test
    fun `title, body and label styles follow the Figma Inter scale`() {
        with(MinkaTypography) {
            assertStyle(titleMedium, 16, 24, 0.15f, FontWeight.SemiBold)
            assertStyle(titleSmall, 14, 20, 0.1f, FontWeight.SemiBold)
            assertStyle(bodyLarge, 16, 24, 0.5f, FontWeight.Normal)
            assertStyle(bodyMedium, 14, 20, 0.25f, FontWeight.Normal)
            assertStyle(bodySmall, 12, 16, 0.4f, FontWeight.Normal)
            assertStyle(labelLarge, 14, 20, 0.1f, FontWeight.Medium)
            assertStyle(labelMedium, 12, 16, 0.5f, FontWeight.Medium)
            assertStyle(labelSmall, 11, 16, 0.5f, FontWeight.Medium)
        }
    }

    @Test
    fun `sticker label is Inter extra bold with wide tracking`() {
        assertStyle(MinkaTheme.typography.sticker, 13, 18, 6f, FontWeight.ExtraBold)
    }

    @Test
    fun `display and title use Fraunces while body uses Inter`() {
        assertNotEquals(MinkaTypography.titleLarge.fontFamily, MinkaTypography.titleMedium.fontFamily)
        assertEquals(MinkaTypography.titleMedium.fontFamily, MinkaTypography.bodyLarge.fontFamily)
    }
}
