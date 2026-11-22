package com.trinitybytes.minka.features.items.domain

import com.trinitybytes.minka.core.domain.Outcome

sealed interface ItemsError {
    data object Offline : ItemsError

    data object Unauthorized : ItemsError

    /** The listing does not exist, is hidden from the viewer, or belongs to someone else. */
    data object NotFound : ItemsError

    /** The draft is not complete enough to publish (Figma 5.05). */
    data object Invalid : ItemsError

    data object Unexpected : ItemsError
}

typealias ItemsOutcome<T> = Outcome<T, ItemsError>

/** Port to the items service. Every call acts as the signed-in user. */
interface ItemsRepository {
    suspend fun feed(): ItemsOutcome<Loaded<Feed>>

    suspend fun search(query: SearchQuery): ItemsOutcome<List<Listing>>

    suspend fun suggestions(text: String): ItemsOutcome<List<String>>

    suspend fun recentSearches(): ItemsOutcome<List<String>>

    suspend fun addRecentSearch(text: String): ItemsOutcome<List<String>>

    suspend fun clearRecentSearches(): ItemsOutcome<Unit>

    suspend fun detail(id: String): ItemsOutcome<Loaded<Listing>>

    suspend fun myListings(status: ListingStatus): ItemsOutcome<Loaded<List<Listing>>>

    suspend fun myDrafts(): ItemsOutcome<List<SavedDraft>>

    /** Creates a draft when [id] is null, otherwise replaces the signed-in user's draft [id]. */
    suspend fun saveDraft(id: String?, draft: ListingDraft): ItemsOutcome<SavedDraft>

    suspend fun draft(id: String): ItemsOutcome<SavedDraft>

    suspend fun publish(id: String): ItemsOutcome<Listing>

    suspend fun setStatus(id: String, status: ListingStatus): ItemsOutcome<Listing>

    /** Removes one of the signed-in user's listings or drafts (Figma 4.08). */
    suspend fun delete(id: String): ItemsOutcome<Unit>

    suspend fun hide(id: String): ItemsOutcome<Unit>

    suspend fun unhide(id: String): ItemsOutcome<Unit>

    suspend fun ownerProfile(id: String): ItemsOutcome<OwnerProfile>
}
