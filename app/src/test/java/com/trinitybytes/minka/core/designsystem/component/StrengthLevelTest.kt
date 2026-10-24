package com.trinitybytes.minka.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class StrengthLevelTest {

    @Test
    fun `each level fills the segments shown in Figma`() {
        assertEquals(1, StrengthLevel.Weak.filledSegments)
        assertEquals(2, StrengthLevel.Medium.filledSegments)
        assertEquals(4, StrengthLevel.Strong.filledSegments)
    }
}
