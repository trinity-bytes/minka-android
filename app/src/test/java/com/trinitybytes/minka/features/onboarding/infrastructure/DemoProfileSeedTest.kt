package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.core.network.InMemoryMockStorage
import com.trinitybytes.minka.features.auth.infrastructure.DEMO_USER_ID
import com.trinitybytes.minka.features.auth.infrastructure.MockAuthServer
import java.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoProfileSeedTest {
    @Test
    fun `the demo account starts in Miraflores so Inicio can show distances`() = runTest {
        val storage = InMemoryMockStorage()
        val server = MockProfileServer(storage, MockAuthServer(storage, Clock.systemUTC()))
        assertEquals("miraflores", server.profileOf(DEMO_USER_ID).district?.id)
    }
}
