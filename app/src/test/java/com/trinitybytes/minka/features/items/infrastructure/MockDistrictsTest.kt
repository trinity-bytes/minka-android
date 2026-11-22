package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.features.onboarding.infrastructure.MockDistricts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDistrictsTest {
    @Test
    fun `distance between centroids is zero in the same district and grows with separation`() {
        assertEquals(0.0, MockDistricts.distanceKm("Miraflores", "miraflores")!!, 0.001)
        assertTrue(MockDistricts.distanceKm("Miraflores", "Barranco")!! > 2.0)
    }

    @Test
    fun `an unknown or missing district has no distance`() {
        assertNull(MockDistricts.distanceKm("Unknown", "Miraflores"))
        assertNull(MockDistricts.distanceKm(null, "Miraflores"))
    }
}
