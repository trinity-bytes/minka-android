package com.trinitybytes.minka.features.items.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.trinitybytes.minka.R
import java.time.Duration
import java.time.Instant

/** How long ago a listing was published, in the units "publicada hace …" (Figma 4.01) and "Publicado hace …" (Figma 11.05) use. */
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

/** The relative age as Spanish text without a verb: "hoy", "ayer", "hace 2 días". */
@Composable
fun ListingAge.text(): String = when (this) {
    ListingAge.Today -> stringResource(R.string.listing_age_today)
    ListingAge.Yesterday -> stringResource(R.string.listing_age_yesterday)
    is ListingAge.Days -> pluralStringResource(R.plurals.listing_age_days, count, count)
    is ListingAge.Weeks -> pluralStringResource(R.plurals.listing_age_weeks, count, count)
    is ListingAge.Months -> pluralStringResource(R.plurals.listing_age_months, count, count)
}
