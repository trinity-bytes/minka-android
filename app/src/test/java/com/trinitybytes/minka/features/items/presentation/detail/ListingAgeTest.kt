package com.trinitybytes.minka.features.items.presentation.detail

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingAgeTest {
    private val now = Instant.parse("2026-11-22T15:00:00Z")

    private fun ageOf(days: Long, hours: Long = 0) = listingAge(now.minus(Duration.ofDays(days).plusHours(hours)), now)

    @Test fun `less than a day is today`() = assertEquals(ListingAge.Today, ageOf(0, 5))

    @Test fun `one day is yesterday`() = assertEquals(ListingAge.Yesterday, ageOf(1))

    @Test fun `days up to a week`() = assertEquals(ListingAge.Days(2), ageOf(2))

    @Test fun `six days is still days`() = assertEquals(ListingAge.Days(6), ageOf(6))

    @Test fun `weeks up to a month`() = assertEquals(ListingAge.Weeks(2), ageOf(15))

    @Test fun `months after thirty days`() = assertEquals(ListingAge.Months(3), ageOf(95))

    @Test fun `a creation date in the future counts as today`() =
        assertEquals(ListingAge.Today, listingAge(now.plusSeconds(60), now))
}
