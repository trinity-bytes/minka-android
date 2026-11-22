package com.trinitybytes.minka.features.items.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingTest {
    @Test
    fun `photos list the cover first and then the gallery in order`() {
        val owner = OwnerSummary("o", "Lucía", "Miraflores")
        val listing = Listing(
            id = "1",
            cover = "a",
            morePhotos = listOf("b", "c"),
            title = "Platos",
            category = Category.HOME,
            condition = Condition.GOOD,
            wants = listOf(Category.BOOKS),
            district = "Miraflores",
            status = ListingStatus.ACTIVE,
            owner = owner,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        assertEquals(listOf("a", "b", "c"), listing.photos)
    }
}
