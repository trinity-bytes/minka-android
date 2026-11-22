package com.trinitybytes.minka.features.items.application

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.infrastructure.ItemsHarness
import com.trinitybytes.minka.features.items.infrastructure.value
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemsUseCasesTest {
    private val harness = ItemsHarness()
    private val repository = harness.repository

    @Test
    fun `resuming a draft returns it for its owner`() = runTest {
        val saved = SaveListingDraft(repository)(null, ItemsHarness.completeDraft()).value()
        assertEquals(saved, ResumeListingDraft(repository)(saved.id).value())
    }

    @Test
    fun `resuming fails for a published listing, an unknown id and another user's draft`() = runTest {
        val listing = harness.publishedDraft()
        val draft = SaveListingDraft(repository)(null, ItemsHarness.completeDraft()).value()
        assertEquals(Outcome.Failure(ItemsError.NotFound), ResumeListingDraft(repository)(listing.id))
        assertEquals(Outcome.Failure(ItemsError.NotFound), ResumeListingDraft(repository)("missing"))
        harness.signInAs("other")
        assertEquals(Outcome.Failure(ItemsError.NotFound), ResumeListingDraft(repository)(draft.id))
    }

    @Test
    fun `pausing and resuming are single reversible steps`() = runTest {
        val listing = harness.publishedDraft()
        assertEquals(ListingStatus.PAUSED, PauseListing(repository)(listing.id).value().status)
        assertEquals(ListingStatus.ACTIVE, ResumeListing(repository)(listing.id).value().status)
    }

    @Test
    fun `hide and undo go through the repository`() = runTest {
        assertEquals(Outcome.Success(Unit), HideListing(repository)("seed-plates"))
        assertEquals(Outcome.Failure(ItemsError.NotFound), GetListingDetail(repository)("seed-plates"))
        assertEquals(Outcome.Success(Unit), UndoHideListing(repository)("seed-plates"))
        assertEquals("seed-plates", GetListingDetail(repository)("seed-plates").value().value.id)
    }
}
