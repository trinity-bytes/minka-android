package com.trinitybytes.minka.features.items.domain

import java.time.Instant

/** The most photos a listing can carry (Figma 5.01). */
const val MAX_PHOTOS = 6

/**
 * A listing being written in the publish flow (Figma 5.01–5.08). Every field can still be missing; the
 * `validate*` functions say what each step needs. The order of [photos] decides the cover: the first one.
 */
data class ListingDraft(
    val photos: List<String> = emptyList(),
    val title: String = "",
    val category: Category? = null,
    val condition: Condition? = null,
    val wants: List<Category> = emptyList(),
    val description: String? = null,
    /** Estimated value in centavos; null means unspecified. */
    val estimatedValue: Long? = null,
    val district: String = "",
    val meetingPoint: String? = null,
    val hideExactLocation: Boolean = true,
    val homeDelivery: Boolean = false,
)

/** A draft kept on the account so it can be resumed from "Perfil > Mis publicaciones" (Figma 5.11, audit P1). */
data class SavedDraft(
    val id: String,
    val draft: ListingDraft,
    val updatedAt: Instant,
)

/** What a step of the publish flow can still be missing (Figma 5.01, 5.03, 5.04, 5.05). */
enum class DraftError {
    PHOTO_REQUIRED,
    TOO_MANY_PHOTOS,
    TITLE_REQUIRED,
    CATEGORY_REQUIRED,
    CONDITION_REQUIRED,
    EXCHANGE_REQUIRED,
    DISTRICT_REQUIRED,
}

/** Step 1: photos, title, category and "Estado". */
fun ListingDraft.validateBasics(): Set<DraftError> = buildSet {
    if (photos.isEmpty() || photos.any { it.isBlank() }) add(DraftError.PHOTO_REQUIRED)
    if (photos.size > MAX_PHOTOS) add(DraftError.TOO_MANY_PHOTOS)
    if (title.isBlank()) add(DraftError.TITLE_REQUIRED)
    if (category == null) add(DraftError.CATEGORY_REQUIRED)
    if (condition == null) add(DraftError.CONDITION_REQUIRED)
}

/** Step 2: at least one "Busca:" category. */
fun ListingDraft.validateExchange(): Set<DraftError> =
    if (wants.isEmpty()) setOf(DraftError.EXCHANGE_REQUIRED) else emptySet()

/** Step 3: the district where the exchange happens. */
fun ListingDraft.validateLocation(): Set<DraftError> =
    if (district.isBlank()) setOf(DraftError.DISTRICT_REQUIRED) else emptySet()

/** The review step (Figma 5.05): everything a draft needs before it can be published. */
fun ListingDraft.validateReview(): Set<DraftError> = validateBasics() + validateExchange() + validateLocation()
