package com.trinitybytes.minka.features.items.presentation.detail

import java.time.Duration
import java.time.Instant

/** How long ago a listing was published, in the units "publicada hace …" of Figma 4.01 uses. */
sealed interface ListingAge {
    data object Today : ListingAge

    data object Yesterday : ListingAge

    data class Days(val count: Int) : ListingAge

    data class Weeks(val count: Int) : ListingAge

    data class Months(val count: Int) : ListingAge
}

private const val DAYS_PER_WEEK = 7
private const val DAYS_PER_MONTH = 30

/** The [ListingAge] of something created at [createdAt], seen at [now]; the future counts as today. */
fun listingAge(createdAt: Instant, now: Instant): ListingAge {
    val days = Duration.between(createdAt, now).toDays().coerceAtLeast(0).toInt()
    return when {
        days == 0 -> ListingAge.Today
        days == 1 -> ListingAge.Yesterday
        days < DAYS_PER_WEEK -> ListingAge.Days(days)
        days < DAYS_PER_MONTH -> ListingAge.Weeks(days / DAYS_PER_WEEK)
        else -> ListingAge.Months(days / DAYS_PER_MONTH)
    }
}
