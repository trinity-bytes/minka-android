package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerProfile
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import com.trinitybytes.minka.features.items.domain.SavedDraft
import com.trinitybytes.minka.features.items.domain.SearchQuery
import com.trinitybytes.minka.features.items.domain.SearchSort
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * Wire format of the items endpoints, shared by the Ktor adapter and the mock server. Enums travel by name
 * and instants as epoch milliseconds, so the domain stays free of serialization concerns.
 */

@Serializable
data class OwnerDto(
    val id: String,
    val name: String,
    val district: String,
    val memberSince: Long? = null,
) {
    fun toDomain() = OwnerSummary(id, name, district, memberSince?.let(Instant::ofEpochMilli))
}

/** A published listing: unlike [DraftDto], its required fields cannot be missing. */
@Serializable
data class ListingDto(
    val id: String,
    val photos: List<String>,
    val title: String,
    val category: String,
    val condition: String,
    val wants: List<String>,
    val district: String,
    val status: String,
    val owner: OwnerDto,
    val createdAt: Long,
    val updatedAt: Long,
    val description: String? = null,
    val estimatedValue: Long? = null,
    val meetingPoint: String? = null,
    val hideExactLocation: Boolean = true,
    val homeDelivery: Boolean = false,
    val distanceKm: Double? = null,
) {
    /** A listing needs a cover, so a payload without photos is rejected instead of half-built. */
    fun toDomain() = Listing(
        id = id,
        cover = photos.first(),
        morePhotos = photos.drop(1),
        title = title,
        category = Category.valueOf(category),
        condition = Condition.valueOf(condition),
        wants = wants.map(Category::valueOf),
        district = district,
        status = ListingStatus.valueOf(status),
        owner = owner.toDomain(),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        description = description,
        estimatedValue = estimatedValue,
        meetingPoint = meetingPoint,
        hideExactLocation = hideExactLocation,
        homeDelivery = homeDelivery,
        distanceKm = distanceKm,
    )
}

/** An editable listing; any field can be missing until the publish flow completes. */
@Serializable
data class DraftDto(
    val photos: List<String> = emptyList(),
    val title: String = "",
    val category: String? = null,
    val condition: String? = null,
    val wants: List<String> = emptyList(),
    val description: String? = null,
    val estimatedValue: Long? = null,
    val district: String = "",
    val meetingPoint: String? = null,
    val hideExactLocation: Boolean = true,
    val homeDelivery: Boolean = false,
) {
    fun toDomain() = ListingDraft(
        photos = photos,
        title = title,
        category = category?.let(Category::valueOf),
        condition = condition?.let(Condition::valueOf),
        wants = wants.map(Category::valueOf),
        description = description,
        estimatedValue = estimatedValue,
        district = district,
        meetingPoint = meetingPoint,
        hideExactLocation = hideExactLocation,
        homeDelivery = homeDelivery,
    )
}

@Serializable
data class SavedDraftDto(val id: String, val draft: DraftDto, val updatedAt: Long) {
    fun toDomain() = SavedDraft(id, draft.toDomain(), Instant.ofEpochMilli(updatedAt))
}

@Serializable
data class FeedDto(val nearby: List<ListingDto>, val byInterest: List<ListingDto>) {
    fun toDomain() = Feed(nearby.map(ListingDto::toDomain), byInterest.map(ListingDto::toDomain))
}

@Serializable
data class OwnerProfileDto(val owner: OwnerDto, val activeListings: List<ListingDto>) {
    fun toDomain() = OwnerProfile(owner.toDomain(), activeListings.map(ListingDto::toDomain))
}

/** A draft to store. The owner comes from the session on the server side, never from the request. */
@Serializable
data class SaveDraftRequest(val draft: DraftDto)

@Serializable
data class SearchRequest(
    val text: String = "",
    val category: String? = null,
    val condition: String? = null,
    val distanceKm: Double? = null,
    val sort: String = SearchSort.NEAREST.name,
)

@Serializable
data class TextRequest(val text: String)

@Serializable
data class StatusRequest(val status: String)

internal fun SearchQuery.toRequest() = SearchRequest(text, category?.name, condition?.name, distanceKm, sort.name)

internal fun ListingDraft.toDto() = DraftDto(
    photos = photos,
    title = title,
    category = category?.name,
    condition = condition?.name,
    wants = wants.map(Category::name),
    description = description,
    estimatedValue = estimatedValue,
    district = district,
    meetingPoint = meetingPoint,
    hideExactLocation = hideExactLocation,
    homeDelivery = homeDelivery,
)

/** Stable `code` values of [com.trinitybytes.minka.core.network.ApiError] for the items endpoints. */
object ItemsErrorCodes {
    const val UNAUTHORIZED = "unauthorized"
    const val NOT_FOUND = "not_found"
    const val INVALID_DRAFT = "invalid_draft"
    const val INVALID_STATUS = "invalid_status"
}
