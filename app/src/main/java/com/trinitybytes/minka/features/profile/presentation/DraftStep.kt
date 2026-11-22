package com.trinitybytes.minka.features.profile.presentation

import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.validateBasics
import com.trinitybytes.minka.features.items.domain.validateExchange
import java.time.Duration
import java.time.Instant

/** Coarse units of "Publicado hace 2 días" (Figma 11.05). */
enum class ListingAgeUnit { NOW, HOURS, DAYS, MONTHS }

data class ListingAge(val unit: ListingAgeUnit, val count: Int)

private const val DAYS_PER_MONTH = 30L

/** How long ago [from] was, as the row subtitle of 11.05 reads it; a future date counts as just now. */
fun listingAge(from: Instant, now: Instant): ListingAge {
    val elapsed = Duration.between(from, now)
    val days = elapsed.toDays()
    return when {
        elapsed.toHours() < 1 -> ListingAge(ListingAgeUnit.NOW, 0)
        days < 1 -> ListingAge(ListingAgeUnit.HOURS, elapsed.toHours().toInt())
        days < DAYS_PER_MONTH -> ListingAge(ListingAgeUnit.DAYS, days.toInt())
        else -> ListingAge(ListingAgeUnit.MONTHS, (days / DAYS_PER_MONTH).toInt())
    }
}

/** The publish step a draft stops at, for "Borrador · paso 1 de 3": the first one that still misses something. */
fun draftStep(draft: ListingDraft): Int = when {
    draft.validateBasics().isNotEmpty() -> 1
    draft.validateExchange().isNotEmpty() -> 2
    else -> 3
}
