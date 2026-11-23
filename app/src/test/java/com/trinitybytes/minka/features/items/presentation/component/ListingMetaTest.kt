package com.trinitybytes.minka.features.items.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListingMetaTest {
    @Test fun `grid meta is district and distance with a decimal comma`() {
        assertEquals("Miraflores · 1,2 km", listingMeta("Miraflores", 1.2))
        assertEquals("San Isidro · 3 km", listingMeta("San Isidro", 3.0))
    }

    @Test fun `distances under a kilometer are shown in meters`() {
        assertEquals("Surco · 800 m", listingMeta("Surco", 0.8))
        assertEquals("Miraflores · 900 m", listingMeta("Miraflores", 0.9))
    }

    @Test fun `row meta adds the condition last`() {
        assertEquals("Barranco · 2,1 km · Como nuevo", listingMeta("Barranco", 2.1, "Como nuevo"))
    }

    @Test fun `unknown or same place distance leaves only the district`() {
        assertEquals("Miraflores", listingMeta("Miraflores", null))
        assertEquals("Miraflores", listingMeta("Miraflores", 0.0))
        assertEquals("Miraflores · Usado", listingMeta("Miraflores", 0.0, "Usado"))
        assertNull(formatDistance(0.01))
    }

    @Test fun `a distance that rounds to a kilometer is shown in kilometers`() {
        assertEquals("1 km", formatDistance(0.99))
    }
}
