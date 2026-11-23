package com.trinitybytes.minka.core.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownTest {
    @Test
    fun `counts from the deadline to zero using the clock`() = runTest {
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(testScheduler.currentTime)
        }
        assertEquals(listOf(3L, 2L, 1L, 0L), countdown(2_500L, clock).toList())
        assertEquals(listOf(0L), countdown(0L, clock).toList())
    }
}
