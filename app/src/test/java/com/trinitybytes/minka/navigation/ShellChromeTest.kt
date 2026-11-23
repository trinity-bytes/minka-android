package com.trinitybytes.minka.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellChromeTest {
    @Test fun `Inicio root shows the bar and the extended Publicar action`() {
        assertEquals(
            ShellChrome(showNavigationBar = true, publish = PublishAction.Extended),
            shellChrome(isRoot = true, homeRoot = true, homeScrolled = false, expanded = false),
        )
    }

    @Test fun `Inicio collapses the action once the feed scrolls`() {
        assertEquals(
            ShellChrome(showNavigationBar = true, publish = PublishAction.Collapsed),
            shellChrome(isRoot = true, homeRoot = true, homeScrolled = true, expanded = false),
        )
    }

    @Test fun `Buscar and Perfil roots show the bar and no action`() {
        assertEquals(
            ShellChrome(showNavigationBar = true, publish = PublishAction.Hidden),
            shellChrome(isRoot = true, homeRoot = false, homeScrolled = false, expanded = false),
        )
    }

    @Test fun `a pushed screen hides the bar and the action on compact widths`() {
        assertEquals(
            ShellChrome(showNavigationBar = false, publish = PublishAction.Hidden),
            shellChrome(isRoot = false, homeRoot = false, homeScrolled = false, expanded = false),
        )
        assertEquals(
            ShellChrome(showNavigationBar = false, publish = PublishAction.Hidden),
            shellChrome(isRoot = false, homeRoot = true, homeScrolled = false, expanded = false),
        )
    }

    @Test fun `the expanded shell leaves the action to the rail`() {
        assertEquals(
            ShellChrome(showNavigationBar = false, publish = PublishAction.Hidden),
            shellChrome(isRoot = true, homeRoot = true, homeScrolled = false, expanded = true),
        )
    }
}
