package com.trinitybytes.minka.features.items.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftValidationTest {
    @Test
    fun `basic step requires photo title category and condition`() {
        val missing = setOf(
            DraftError.PHOTO_REQUIRED,
            DraftError.TITLE_REQUIRED,
            DraftError.CATEGORY_REQUIRED,
            DraftError.CONDITION_REQUIRED,
        )
        assertEquals(missing, ListingDraft().validateBasics())
    }

    @Test
    fun `basic step accepts a complete draft and rejects more than six photos`() {
        val draft = ListingDraft(
            photos = listOf("photo"),
            title = "Platos",
            category = Category.HOME,
            condition = Condition.GOOD,
        )
        assertTrue(draft.validateBasics().isEmpty())
        assertEquals(setOf(DraftError.TOO_MANY_PHOTOS), draft.copy(photos = List(7) { "$it" }).validateBasics())
    }

    @Test
    fun `a blank photo does not count as a photo`() {
        assertTrue(DraftError.PHOTO_REQUIRED in ListingDraft(photos = listOf(" ")).validateBasics())
    }

    @Test
    fun `exchange step requires at least one desired category`() {
        assertEquals(setOf(DraftError.EXCHANGE_REQUIRED), ListingDraft().validateExchange())
        assertTrue(ListingDraft(wants = listOf(Category.BOOKS)).validateExchange().isEmpty())
    }

    @Test
    fun `location requires a district and review combines every step`() {
        assertEquals(setOf(DraftError.DISTRICT_REQUIRED), ListingDraft().validateLocation())
        assertTrue(ListingDraft(district = "Miraflores").validateLocation().isEmpty())
        assertEquals(
            setOf(
                DraftError.PHOTO_REQUIRED,
                DraftError.TITLE_REQUIRED,
                DraftError.CATEGORY_REQUIRED,
                DraftError.CONDITION_REQUIRED,
                DraftError.EXCHANGE_REQUIRED,
                DraftError.DISTRICT_REQUIRED,
            ),
            ListingDraft().validateReview(),
        )
    }
}
