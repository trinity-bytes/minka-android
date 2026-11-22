package com.trinitybytes.minka.features.profile.presentation

import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingAgeTest {
    private val now = Instant.parse("2026-11-22T12:00:00Z")

    @Test fun `less than an hour is just now`() {
        assertEquals(ListingAge(ListingAgeUnit.NOW, 0), listingAge(now.minusSeconds(600), now))
    }

    @Test fun `under a day counts hours`() {
        assertEquals(ListingAge(ListingAgeUnit.HOURS, 5), listingAge(now.minusSeconds(5 * 3600L + 30), now))
    }

    @Test fun `under a month counts days`() {
        assertEquals(ListingAge(ListingAgeUnit.DAYS, 2), listingAge(now.minusSeconds(2 * 86_400L + 100), now))
    }

    @Test fun `older counts months and a future date is just now`() {
        assertEquals(ListingAge(ListingAgeUnit.MONTHS, 3), listingAge(now.minusSeconds(95 * 86_400L), now))
        assertEquals(ListingAge(ListingAgeUnit.NOW, 0), listingAge(now.plusSeconds(60), now))
    }

    @Test fun `a draft is on the step that still misses something`() {
        val photos = ListingDraft(
            photos = listOf("p"),
            title = "Mesa",
            category = Category.HOME,
            condition = Condition.GOOD,
        )
        assertEquals(1, draftStep(ListingDraft()))
        assertEquals(2, draftStep(photos))
        assertEquals(3, draftStep(photos.copy(wants = listOf(Category.BOOKS))))
    }
}
