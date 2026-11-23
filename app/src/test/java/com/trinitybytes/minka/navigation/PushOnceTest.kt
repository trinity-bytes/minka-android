package com.trinitybytes.minka.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PushOnceTest {
    @Test
    fun `a double tap pushes the same key once`() {
        val stack = mutableListOf("home")
        pushOnce(stack, "detail-1")
        pushOnce(stack, "detail-1")
        assertEquals(listOf("home", "detail-1"), stack)
    }

    @Test
    fun `a different key is still pushed`() {
        val stack = mutableListOf("home", "detail-1")
        pushOnce(stack, "owner-1")
        assertEquals(listOf("home", "detail-1", "owner-1"), stack)
    }

    @Test
    fun `popping only removes the expected top so a double close never pops the shell`() {
        val stack = mutableListOf("shell", "publish")
        popIf(stack) { it == "publish" }
        popIf(stack) { it == "publish" }
        assertEquals(listOf("shell"), stack)
    }
}
