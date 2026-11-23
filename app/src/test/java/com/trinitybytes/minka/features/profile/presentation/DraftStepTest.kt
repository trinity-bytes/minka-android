package com.trinitybytes.minka.features.profile.presentation

import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftStepTest {
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
