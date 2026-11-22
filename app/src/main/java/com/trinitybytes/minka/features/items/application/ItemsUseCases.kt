package com.trinitybytes.minka.features.items.application

import com.trinitybytes.minka.features.items.domain.ItemsRepository
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.SearchQuery
import javax.inject.Inject

/** Inicio shelves: nearby and by interest, from the cache when offline (Figma 2.01-2.05). */
class GetFeed @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke() = repository.feed()
}

/** Runs a search with text, category, condition, distance and sort (Figma 3.02-3.05). */
class SearchListings @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(query: SearchQuery) = repository.search(query)
}

/** Titles that match the text being typed (Figma 3.03). */
class GetSearchSuggestions @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(text: String) = repository.suggestions(text)
}

/** The viewer's last searches, newest first (Figma 3.01). */
class GetRecentSearches @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke() = repository.recentSearches()
}

/** Remembers a search the viewer submitted and returns the updated history. */
class AddRecentSearch @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(text: String) = repository.addRecentSearch(text)
}

/** Empties the search history (Figma 3.01). */
class ClearRecentSearches @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke() = repository.clearRecentSearches()
}

/** One published listing (Figma 4.01); fails with NotFound for drafts, hidden or unknown ids. */
class GetListingDetail @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.detail(id)
}

/** The signed-in user's active or paused listings (Figma 11.05). */
class GetMyListings @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(status: ListingStatus) = repository.myListings(status)
}

/** The signed-in user's drafts, newest first (audit P1, Figma 5.11). */
class GetMyDrafts @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke() = repository.myDrafts()
}

/** Saves the draft as it is, complete or not ("Guardar borrador", Figma 5.01-5.08). */
class SaveListingDraft @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String?, draft: ListingDraft) = repository.saveDraft(id, draft)
}

/** Loads a draft to continue it; fails with NotFound unless it is a draft of the signed-in user. */
class ResumeListingDraft @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.draft(id)
}

/** Publishes a complete draft (Figma 5.09); fails with Invalid when the review step would not pass. */
class PublishListing @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.publish(id)
}

/** Hides an active listing from others; reversible and without confirmation (audit P4). */
class PauseListing @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.setStatus(id, ListingStatus.PAUSED)
}

/** Makes a paused listing visible again (audit P4). */
class ResumeListing @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.setStatus(id, ListingStatus.ACTIVE)
}

/** Deletes one of the signed-in user's listings or drafts after the confirmation of Figma 4.08. */
class DeleteListing @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

/** Hides someone else's listing from the viewer's feed and search (audit finding 13). */
class HideListing @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.hide(id)
}

/** Brings back a listing hidden with [HideListing] (the snackbar "Deshacer"). */
class UndoHideListing @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.unhide(id)
}

/** The public profile of a listing owner (Figma 11.02). */
class GetOwnerProfile @Inject constructor(private val repository: ItemsRepository) {
    suspend operator fun invoke(id: String) = repository.ownerProfile(id)
}
