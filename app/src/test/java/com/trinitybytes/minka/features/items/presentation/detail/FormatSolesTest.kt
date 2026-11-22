package com.trinitybytes.minka.features.items.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatSolesTest {
    @Test fun `whole soles have no decimals`() = assertEquals("120", formatSoles(12_000))

    @Test fun `cents use the Spanish decimal comma`() = assertEquals("120,50", formatSoles(12_050))
}
