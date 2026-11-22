package com.trinitybytes.minka.features.items.domain

import java.time.Instant

/** What an item can be filed under: the categories of Figma 3.01 and the "Busca:" chips of 5.03. */
enum class Category {
    HOME,
    CLOTHING,
    BOOKS,
    TECHNOLOGY,
    PLANTS,
    SPORTS,
    TOOLS,
    SERVICES,
    ANY,
}

/** "Estado" of an item (Figma 5.01), from best to worst; the search filter reads it as "at least". */
enum class Condition {
    NEW,
    LIKE_NEW,
    GOOD,
    USED,
}

/** Visibility of a published listing: 4.03 and 11.05 pause and resume it; drafts are [SavedDraft]s. */
enum class ListingStatus {
    ACTIVE,
    PAUSED,
}

/** The person who published a listing, as 4.01 and 11.02 show them (no rating or badge before phase 4). */
data class OwnerSummary(
    val id: String,
    val name: String,
    val district: String,
    /** Null when the account has no known creation date; the profile then omits "Miembro desde". */
    val memberSince: Instant? = null,
)

/**
 * A published listing (Figma 2.02, 4.01, 11.05). Every field the publish flow requires is non-null, unlike
 * the editable [ListingDraft] it came from.
 */
data class Listing(
    val id: String,
    /** First photo, shown on cards and as the large photo of 4.01. */
    val cover: String,
    /** The remaining photos, in gallery order. */
    val morePhotos: List<String>,
    val title: String,
    val category: Category,
    val condition: Condition,
    /** What the owner wants in exchange ("Busca:"); never empty for a published listing. */
    val wants: List<Category>,
    val district: String,
    val status: ListingStatus,
    val owner: OwnerSummary,
    val createdAt: Instant,
    val updatedAt: Instant,
    val description: String? = null,
    /** Estimated value in centavos. */
    val estimatedValue: Long? = null,
    val meetingPoint: String? = null,
    val hideExactLocation: Boolean = true,
    val homeDelivery: Boolean = false,
    /** Approximate distance from the viewer's district; null when it cannot be computed. */
    val distanceKm: Double? = null,
) {
    /** The cover followed by the other photos. */
    val photos: List<String> get() = listOf(cover) + morePhotos
}

/** The public profile of a listing owner (Figma 11.02): who they are and what they offer now. */
data class OwnerProfile(
    val owner: OwnerSummary,
    val activeListings: List<Listing>,
)
