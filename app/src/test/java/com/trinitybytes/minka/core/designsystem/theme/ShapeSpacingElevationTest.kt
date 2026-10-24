package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the Forma, Espaciado and Elevación sections of the Figma Foundations page. */
class ShapeSpacingElevationTest {

    @Test
    fun `material shapes follow the Figma radius scale`() {
        with(MinkaMaterialShapes) {
            assertEquals(RoundedCornerShape(6.dp), extraSmall)
            assertEquals(RoundedCornerShape(10.dp), small)
            assertEquals(RoundedCornerShape(16.dp), medium)
            assertEquals(RoundedCornerShape(24.dp), large)
            assertEquals(RoundedCornerShape(28.dp), extraLarge)
        }
    }

    @Test
    fun `minka shapes cover the radii material has no slot for`() {
        assertEquals(RoundedCornerShape(12.dp), MinkaShapes.control)
        assertEquals(RoundedCornerShape(38.dp), MinkaShapes.bento)
        assertEquals(CircleShape, MinkaShapes.full)
    }

    @Test
    fun `spacing follows the 4 dp grid`() {
        val scale = with(MinkaSpacing) { listOf(s4, s8, s12, s16, s20, s24, s32, s40, s48, s56, s64) }
        assertEquals(listOf(4, 8, 12, 16, 20, 24, 32, 40, 48, 56, 64).map { it.dp }, scale)
    }

    @Test
    fun `elevations are the Figma ink-tinted drop shadows`() {
        with(MinkaElevation.lg) {
            assertEquals(24.dp, radius)
            assertEquals((-8).dp, spread)
            assertEquals(DpOffset(0.dp, 8.dp), offset)
            assertEquals(Color(0x1F10291C), color)
        }
        with(MinkaElevation.modal) {
            assertEquals(64.dp, radius)
            assertEquals((-16).dp, spread)
            assertEquals(DpOffset(0.dp, 24.dp), offset)
            assertEquals(Color(0x470A1D13), color)
        }
        assertEquals(Color(0x0D10291C), MinkaElevation.sm.color)
        assertEquals(Color(0x0F10291C), MinkaElevation.md.color)
        assertEquals(Color(0x2910291C), MinkaElevation.xl.color)
        assertEquals(Color(0x2E10291C), MinkaElevation.lift.color)
    }
}
