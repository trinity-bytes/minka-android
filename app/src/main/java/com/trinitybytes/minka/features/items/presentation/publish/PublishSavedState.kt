package com.trinitybytes.minka.features.items.presentation.publish

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.ListingDraft

private const val KEY_PAGE = "publish_page"
private const val KEY_DRAFT_ID = "publish_draft_id"
private const val KEY_RETURN = "publish_return_to_review"
private const val DRAFT = "publish_draft_"
private const val BASELINE = "publish_baseline_"

/** The form as it was restored after process death, with what the next save and close compare against. */
internal data class RestoredForm(
    val draft: ListingDraft,
    val baseline: ListingDraft,
    val page: PublishPage,
    val draftId: String?,
    val returnToReview: Boolean,
)

/**
 * The form lives in the ViewModel, so [SavedStateHandle] keeps it across process death. A draft is
 * not parcelable, so each field is stored as a plain value; photos are app-private file URIs that
 * outlive the process.
 */
internal fun SavedStateHandle.saveForm(
    draft: ListingDraft,
    baseline: ListingDraft,
    page: PublishPage,
    draftId: String?,
    returnToReview: Boolean,
) {
    set(KEY_PAGE, page.name)
    set(KEY_DRAFT_ID, draftId)
    set(KEY_RETURN, returnToReview)
    write(DRAFT, draft)
    write(BASELINE, baseline)
}

internal fun SavedStateHandle.restoreForm(): RestoredForm? {
    val page = get<String>(KEY_PAGE)?.let { runCatching { PublishPage.valueOf(it) }.getOrNull() } ?: return null
    val draft = read(DRAFT) ?: return null
    val baseline = read(BASELINE) ?: return null
    return RestoredForm(draft, baseline, page, get<String>(KEY_DRAFT_ID), get<Boolean>(KEY_RETURN) ?: false)
}

internal fun SavedStateHandle.clearForm() {
    listOf(KEY_PAGE, KEY_DRAFT_ID, KEY_RETURN).forEach { remove<Any>(it) }
    clear(DRAFT)
    clear(BASELINE)
}

private val FIELDS = listOf(
    "photos",
    "title",
    "category",
    "condition",
    "wants",
    "description",
    "value",
    "district",
    "meeting",
    "hide",
    "delivery",
)

private fun SavedStateHandle.clear(prefix: String) = FIELDS.forEach { remove<Any>(prefix + it) }

private fun SavedStateHandle.write(prefix: String, draft: ListingDraft) {
    set(prefix + "photos", ArrayList(draft.photos))
    set(prefix + "title", draft.title)
    set(prefix + "category", draft.category?.name)
    set(prefix + "condition", draft.condition?.name)
    set(prefix + "wants", ArrayList(draft.wants.map { it.name }))
    set(prefix + "description", draft.description)
    set(prefix + "value", draft.estimatedValue)
    set(prefix + "district", draft.district)
    set(prefix + "meeting", draft.meetingPoint)
    set(prefix + "hide", draft.hideExactLocation)
    set(prefix + "delivery", draft.homeDelivery)
}

private fun SavedStateHandle.read(prefix: String): ListingDraft? {
    val title = get<String>(prefix + "title") ?: return null
    return ListingDraft(
        photos = get<ArrayList<String>>(prefix + "photos").orEmpty(),
        title = title,
        category = get<String>(prefix + "category")?.let { runCatching { Category.valueOf(it) }.getOrNull() },
        condition = get<String>(prefix + "condition")?.let { runCatching { Condition.valueOf(it) }.getOrNull() },
        wants = get<ArrayList<String>>(prefix + "wants").orEmpty()
            .mapNotNull { runCatching { Category.valueOf(it) }.getOrNull() },
        description = get<String>(prefix + "description"),
        estimatedValue = get<Long>(prefix + "value"),
        district = get<String>(prefix + "district").orEmpty(),
        meetingPoint = get<String>(prefix + "meeting"),
        hideExactLocation = get<Boolean>(prefix + "hide") ?: true,
        homeDelivery = get<Boolean>(prefix + "delivery") ?: false,
    )
}
