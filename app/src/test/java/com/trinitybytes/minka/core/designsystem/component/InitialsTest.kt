package com.trinitybytes.minka.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {

    @Test
    fun `initials take the first and last words and skip symbols`() {
        assertEquals("AQ", initialsOf("Ana Quispe"))
        assertEquals("AT", initialsOf("Ana Q. · Trabajo"))
        assertEquals("L", initialsOf("luis"))
        assertEquals("", initialsOf(" · "))
    }
}
