package com.trinitybytes.minka.features.auth.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

/** Figma 1.11b shows a phone grouped in threes: "987 654 321". */
class PhoneFormatTest {

    @Test
    fun `a nine digit phone is grouped in threes`() {
        assertEquals("987 654 321", groupPhoneDigits("987654321"))
    }

    @Test
    fun `a shorter tail stays as the last group`() {
        assertEquals("987 654 32", groupPhoneDigits("98765432"))
    }
}
