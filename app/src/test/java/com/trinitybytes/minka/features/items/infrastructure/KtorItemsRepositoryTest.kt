package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.SearchQuery
import com.trinitybytes.minka.features.items.domain.SearchSort
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorItemsRepositoryTest {
    private val harness = ItemsHarness()
    private val repository = harness.repository

    @Test
    fun `feed lists other people's active listings nearest first and leaves out my own`() = runTest {
        harness.signInAs("user-ana")
        harness.profiles.saveDistrict("barranco")
        val feed = repository.feed().value()
        assertFalse(feed.fromCache)
        assertEquals(listOf("seed-plates"), feed.value.nearby.map { it.id })
        assertTrue(feed.value.nearby.single().distanceKm!! > 2.0)
        assertEquals(listOf("seed-ana-bike"), repository.myListings(ListingStatus.ACTIVE).value().value.map { it.id })
    }

    @Test
    fun `feed by interest keeps only the categories the viewer chose`() = runTest {
        assertTrue(repository.feed().value().value.byInterest.isEmpty())
        harness.profiles.saveInterests(listOf("hogar"))
        val feed = repository.feed().value().value
        assertEquals(listOf("seed-plates"), feed.byInterest.map { it.id })
        harness.profiles.saveInterests(listOf("libros"))
        assertTrue(repository.feed().value().value.byInterest.isEmpty())
        assertEquals(listOf("seed-plates", "seed-ana-bike"), repository.feed().value().value.nearby.map { it.id })
    }

    @Test
    fun `offline feed and detail come from the cache and say so`() = runTest {
        val online = repository.feed().value().value
        harness.offline = true
        val cached = repository.feed().value()
        assertTrue(cached.fromCache)
        assertEquals(online, cached.value)
        val detail = repository.detail("seed-plates").value()
        assertTrue(detail.fromCache)
        assertEquals(online.nearby.first { it.id == "seed-plates" }, detail.value)
    }

    @Test
    fun `the cache is scoped to the signed in user`() = runTest {
        repository.feed().value()
        harness.offline = true
        harness.signInAs("other")
        assertEquals(Outcome.Failure(ItemsError.Offline), repository.feed())
        assertEquals(Outcome.Failure(ItemsError.Offline), repository.detail("seed-plates"))
    }

    @Test
    fun `a broken cache never fails an online request and offline then reports offline`() = runTest {
        harness.cache.broken = true
        assertFalse(repository.feed().value().fromCache)
        harness.offline = true
        assertEquals(Outcome.Failure(ItemsError.Offline), repository.feed())
    }

    @Test
    fun `my listings fall back to the cache offline`() = runTest {
        harness.signInAs("user-ana")
        val online = repository.myListings(ListingStatus.PAUSED).value().value
        harness.offline = true
        val cached = repository.myListings(ListingStatus.PAUSED).value()
        assertTrue(cached.fromCache)
        assertEquals(online, cached.value)
        assertEquals(listOf("seed-ana-table"), cached.value.map { it.id })
    }

    @Test
    fun `a draft is saved resumed revised published paused resumed and deleted`() = runTest {
        val saved = repository.saveDraft(null, ItemsHarness.completeDraft()).value()
        assertEquals(listOf(saved), repository.myDrafts().value())
        assertEquals(saved, repository.draft(saved.id).value())
        val revised = repository.saveDraft(saved.id, ItemsHarness.completeDraft("Platos verdes")).value()
        assertEquals(saved.id, revised.id)
        assertEquals("Platos verdes", repository.draft(saved.id).value().draft.title)
        val published = repository.publish(saved.id).value()
        assertEquals(ListingStatus.ACTIVE, published.status)
        assertEquals("photo-1", published.cover)
        assertEquals(listOf("photo-2"), published.morePhotos)
        assertTrue(repository.myDrafts().value().isEmpty())
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.draft(saved.id))
        assertEquals(ListingStatus.PAUSED, repository.setStatus(saved.id, ListingStatus.PAUSED).value().status)
        assertEquals(listOf(saved.id), repository.myListings(ListingStatus.PAUSED).value().value.map { it.id })
        assertEquals(ListingStatus.ACTIVE, repository.setStatus(saved.id, ListingStatus.ACTIVE).value().status)
        assertEquals(Outcome.Success(Unit), repository.delete(saved.id))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.detail(saved.id))
    }

    @Test
    fun `an incomplete draft cannot be published and stays a draft`() = runTest {
        val incomplete = repository.saveDraft(null, ListingDraft()).value()
        assertEquals(Outcome.Failure(ItemsError.Invalid), repository.publish(incomplete.id))
        assertEquals(listOf(incomplete.id), repository.myDrafts().value().map { it.id })
    }

    @Test
    fun `a draft is not a listing`() = runTest {
        val saved = repository.saveDraft(null, ItemsHarness.completeDraft()).value()
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.detail(saved.id))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.setStatus(saved.id, ListingStatus.PAUSED))
        assertTrue(repository.myListings(ListingStatus.ACTIVE).value().value.none { it.id == saved.id })
    }

    @Test
    fun `nobody else can read edit publish pause or delete my draft and listings`() = runTest {
        val draft = repository.saveDraft(null, ItemsHarness.completeDraft()).value()
        val listing = harness.publishedDraft("Otra cosa")
        harness.signInAs("other")
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.draft(draft.id))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.saveDraft(draft.id, ListingDraft()))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.publish(draft.id))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.detail(draft.id))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.delete(draft.id))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.setStatus(listing.id, ListingStatus.PAUSED))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.delete(listing.id))
        assertTrue(repository.myDrafts().value().isEmpty())
        assertEquals(ListingStatus.ACTIVE, repository.detail(listing.id).value().value.status)
        harness.signInAs("ana")
        assertEquals("Platos azules", repository.draft(draft.id).value().draft.title)
    }

    @Test
    fun `search combines text category condition and distance`() = runTest {
        harness.signInAs("other")
        val barranco = ItemsHarness.completeDraft().copy(district = "Barranco")
        val posted = repository.publish(repository.saveDraft(null, barranco).value().id).value()
        harness.signInAs("user-ana")
        harness.profiles.saveDistrict("miraflores")
        suspend fun ids(query: SearchQuery) = repository.search(query).value().map { it.id }
        assertEquals(listOf(posted.id), ids(SearchQuery(text = "azules", category = Category.HOME)))
        assertEquals(listOf("seed-plates"), ids(SearchQuery(text = "cerámica")))
        assertEquals(listOf("seed-plates"), ids(SearchQuery(text = "hondos")))
        assertTrue(ids(SearchQuery(text = "azules", category = Category.SPORTS)).isEmpty())
        assertTrue(ids(SearchQuery(text = "azules", condition = Condition.NEW)).isEmpty())
        assertTrue(posted.id in ids(SearchQuery(condition = Condition.GOOD)))
        assertEquals(listOf("seed-plates"), ids(SearchQuery(distanceKm = 1.0)))
        assertEquals(setOf("seed-plates", posted.id), ids(SearchQuery(distanceKm = 5.0)).toSet())
    }

    @Test
    fun `search sorts nearest first or newest first`() = runTest {
        harness.signInAs("other")
        harness.profiles.saveDistrict("miraflores")
        val posted = harness.publishedDraft()
        harness.signInAs("user-ana")
        val seedOnly = repository.search(SearchQuery(sort = SearchSort.NEWEST)).value().map { it.id }
        assertEquals(listOf(posted.id, "seed-plates"), seedOnly)
        val nearest = repository.search(SearchQuery(sort = SearchSort.NEAREST)).value()
        assertEquals(nearest.sortedBy { it.distanceKm ?: Double.MAX_VALUE }, nearest)
    }

    @Test
    fun `suggestions follow matching titles and a blank text suggests nothing`() = runTest {
        assertTrue("Juego de platos de cerámica" in repository.suggestions("cerámica").value())
        assertTrue(repository.suggestions("").value().isEmpty())
        assertTrue(repository.suggestions("zzz").value().isEmpty())
    }

    @Test
    fun `recent searches keep the newest first without repeats and can be cleared`() = runTest {
        assertTrue(repository.recentSearches().value().isEmpty())
        repository.addRecentSearch("platos")
        repository.addRecentSearch("bici")
        assertEquals(listOf("PLATOS", "bici"), repository.addRecentSearch("PLATOS").value())
        assertEquals(listOf("PLATOS", "bici"), repository.recentSearches().value())
        assertEquals(listOf("PLATOS", "bici"), repository.addRecentSearch("  ").value())
        repeat(12) { repository.addRecentSearch("busqueda $it") }
        assertEquals(10, repository.recentSearches().value().size)
        assertEquals(Outcome.Success(Unit), repository.clearRecentSearches())
        assertTrue(repository.recentSearches().value().isEmpty())
    }

    @Test
    fun `hiding removes a listing from feed search and detail and undo brings it back`() = runTest {
        assertEquals(Outcome.Success(Unit), repository.hide("seed-plates"))
        assertTrue(repository.feed().value().value.nearby.none { it.id == "seed-plates" })
        assertTrue(repository.search(SearchQuery()).value().none { it.id == "seed-plates" })
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.detail("seed-plates"))
        harness.offline = true
        assertTrue(repository.feed().value().value.nearby.none { it.id == "seed-plates" })
        harness.offline = false
        assertEquals(Outcome.Success(Unit), repository.unhide("seed-plates"))
        assertTrue(repository.feed().value().value.nearby.any { it.id == "seed-plates" })
        assertTrue(repository.detail("seed-plates").value().value.id == "seed-plates")
    }

    @Test
    fun `I cannot hide my own listing or an unknown one`() = runTest {
        harness.signInAs("user-ana")
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.hide("seed-ana-bike"))
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.hide("missing"))
    }

    @Test
    fun `owner profile lists active listings and leaves out member since when unknown`() = runTest {
        val lucia = repository.ownerProfile("seed-lucia").value()
        assertEquals("Lucía Torres", lucia.owner.name)
        assertTrue(lucia.owner.memberSince != null)
        assertEquals(listOf("seed-plates"), lucia.activeListings.map { it.id })
        val ana = repository.ownerProfile("user-ana").value()
        assertNull(ana.owner.memberSince)
        assertEquals(listOf("seed-ana-bike"), ana.activeListings.map { it.id })
        assertEquals(Outcome.Failure(ItemsError.NotFound), repository.ownerProfile("nobody"))
    }

    @Test
    fun `owner profile survives when every listing is paused`() = runTest {
        val listing = harness.publishedDraft()
        repository.setStatus(listing.id, ListingStatus.PAUSED)
        val profile = repository.ownerProfile("ana").value()
        assertEquals("ana", profile.owner.id)
        assertNull(profile.owner.memberSince)
        assertTrue(profile.activeListings.isEmpty())
    }

    @Test
    fun `the demo account finds its seeded listings in every status`() = runTest {
        harness.signInAs("user-ana")
        assertEquals(1, repository.myListings(ListingStatus.ACTIVE).value().value.size)
        assertEquals(1, repository.myListings(ListingStatus.PAUSED).value().value.size)
        assertEquals(listOf("seed-ana-draft"), repository.myDrafts().value().map { it.id })
        harness.signInAs("other")
        assertTrue(repository.myDrafts().value().isEmpty())
        assertTrue(repository.myListings(ListingStatus.ACTIVE).value().value.isEmpty())
    }

    @Test
    fun `distance follows the viewer's district`() = runTest {
        harness.profiles.saveDistrict("barranco")
        assertTrue(repository.detail("seed-plates").value().value.distanceKm!! > 2.0)
        harness.profiles.saveDistrict("miraflores")
        assertEquals(0.0, repository.detail("seed-plates").value().value.distanceKm!!, 0.001)
    }

    @Test
    fun `every call fails as unauthorized without a session`() = runTest {
        harness.sessions.clear()
        assertEquals(Outcome.Failure(ItemsError.Unauthorized), repository.feed())
        assertEquals(Outcome.Failure(ItemsError.Unauthorized), repository.publish("x"))
    }

    @Test
    fun `writes fail as offline without touching the cache`() = runTest {
        harness.offline = true
        assertEquals(Outcome.Failure(ItemsError.Offline), repository.saveDraft(null, ItemsHarness.completeDraft()))
        assertEquals(Outcome.Failure(ItemsError.Offline), repository.setStatus("seed-ana-bike", ListingStatus.PAUSED))
    }
}
