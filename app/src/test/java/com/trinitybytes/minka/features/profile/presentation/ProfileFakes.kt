package com.trinitybytes.minka.features.profile.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ItemsOutcome
import com.trinitybytes.minka.features.items.domain.ItemsRepository
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.Loaded
import com.trinitybytes.minka.features.items.domain.OwnerProfile
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import com.trinitybytes.minka.features.items.domain.SavedDraft
import com.trinitybytes.minka.features.items.domain.SearchQuery
import java.time.Instant

val ANA = OwnerSummary(id = "ana", name = "Ana Quispe", district = "Miraflores")

fun listing(id: String, status: ListingStatus = ListingStatus.ACTIVE) = Listing(
    id = id,
    cover = "cover-$id",
    morePhotos = emptyList(),
    title = "Item $id",
    category = Category.HOME,
    condition = Condition.GOOD,
    wants = listOf(Category.BOOKS),
    district = "Miraflores",
    status = status,
    owner = ANA,
    createdAt = Instant.parse("2026-11-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-11-01T10:00:00Z"),
)

fun savedDraft(id: String, title: String = "") = SavedDraft(
    id = id,
    draft = ListingDraft(title = title),
    updatedAt = Instant.parse("2026-11-02T10:00:00Z"),
)

/** In-memory items service for the profile ViewModels; only the calls the profile makes are implemented. */
class FakeItemsRepository : ItemsRepository {
    val listings = mutableListOf<Listing>()
    val drafts = mutableListOf<SavedDraft>()
    var profile: ItemsOutcome<OwnerProfile> = Outcome.Failure(ItemsError.NotFound)
    var failure: ItemsError? = null
    var fromCache = false
    val calls = mutableListOf<String>()

    private fun <T> result(value: T): ItemsOutcome<T> = failure?.let { Outcome.Failure(it) } ?: Outcome.Success(value)

    override suspend fun myListings(status: ListingStatus): ItemsOutcome<Loaded<List<Listing>>> =
        result(Loaded(listings.filter { it.status == status }, fromCache))

    override suspend fun myDrafts(): ItemsOutcome<List<SavedDraft>> = result(drafts.toList())

    override suspend fun setStatus(id: String, status: ListingStatus): ItemsOutcome<Listing> {
        calls += "setStatus:$id:$status"
        val index = listings.indexOfFirst { it.id == id }
        if (failure != null || index < 0) return Outcome.Failure(failure ?: ItemsError.NotFound)
        listings[index] = listings[index].copy(status = status)
        return Outcome.Success(listings[index])
    }

    override suspend fun delete(id: String): ItemsOutcome<Unit> {
        calls += "delete:$id"
        if (failure != null) return Outcome.Failure(failure!!)
        listings.removeAll { it.id == id }
        drafts.removeAll { it.id == id }
        return Outcome.Success(Unit)
    }

    override suspend fun ownerProfile(id: String): ItemsOutcome<OwnerProfile> {
        calls += "ownerProfile:$id"
        return profile
    }

    override suspend fun feed(): ItemsOutcome<Loaded<Feed>> = unused()
    override suspend fun search(query: SearchQuery): ItemsOutcome<List<Listing>> = unused()
    override suspend fun suggestions(text: String): ItemsOutcome<List<String>> = unused()
    override suspend fun recentSearches(): ItemsOutcome<List<String>> = unused()
    override suspend fun addRecentSearch(text: String): ItemsOutcome<List<String>> = unused()
    override suspend fun clearRecentSearches(): ItemsOutcome<Unit> = unused()
    override suspend fun detail(id: String): ItemsOutcome<Loaded<Listing>> = unused()
    override suspend fun saveDraft(id: String?, draft: ListingDraft): ItemsOutcome<SavedDraft> = unused()
    override suspend fun draft(id: String): ItemsOutcome<SavedDraft> = unused()
    override suspend fun publish(id: String): ItemsOutcome<Listing> = unused()
    override suspend fun hide(id: String): ItemsOutcome<Unit> = unused()
    override suspend fun unhide(id: String): ItemsOutcome<Unit> = unused()

    private fun unused(): Nothing = error("The profile never calls this")
}
