package com.trinitybytes.minka.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TabNavigationTest {
    @Test fun `switching tabs preserves stacks and reselecting clears only current stack`() {
        val home = mutableListOf("home", "home-detail")
        val search = mutableListOf("search", "search-detail")
        val profile = mutableListOf("profile", "profile-detail")
        val stacks = mapOf("home" to home, "search" to search, "profile" to profile)
        assertEquals("search", selectTab("home", "search", stacks))
        assertEquals(listOf("home", "home-detail"), home)
        assertEquals(listOf("search", "search-detail"), search)
        assertEquals("search", selectTab("search", "search", stacks))
        assertEquals(listOf("search"), search)
        assertEquals(listOf("profile", "profile-detail"), profile)
    }
}
