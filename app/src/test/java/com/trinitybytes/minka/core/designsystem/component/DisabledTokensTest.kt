package com.trinitybytes.minka.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class DisabledTokensTest {
    @Test fun materialDisabledAlphaValues() {
        assertEquals(0.38f, MinkaDisabled.ContentAlpha)
        assertEquals(0.12f, MinkaDisabled.ContainerAlpha)
    }
}
