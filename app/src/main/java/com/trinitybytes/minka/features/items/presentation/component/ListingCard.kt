package com.trinitybytes.minka.features.items.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaBadge
import com.trinitybytes.minka.core.designsystem.component.MinkaBadgeTone
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Listing
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The meta line of a card: "Miraflores · 1,2 km" on the grid card (Figma 2.02), plus the condition on the
 * row card (3.05). A missing or same-district distance is left out.
 */
fun listingMeta(district: String, distanceKm: Double?, condition: String? = null): String =
    listOfNotNull(district, distanceKm?.let(::formatDistance), condition).joinToString(" · ")

private const val METERS_PER_KM = 1000
private const val METER_STEP = 50
private const val MIN_SHOWN_KM = 0.05

/** "800 m" under a kilometer, otherwise "1,2 km" with the Spanish decimal comma; null when it is the same place. */
fun formatDistance(km: Double): String? {
    if (km < MIN_SHOWN_KM) return null
    val meters = (km * METERS_PER_KM / METER_STEP).roundToInt() * METER_STEP
    if (meters < METERS_PER_KM) return "$meters m"
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("es"))
    format.maximumFractionDigits = 1
    return "${format.format(km)} km"
}

/** Grid card for Inicio and row card for search results (Figma 2.02 and 3.05). */
enum class ListingCardLayout { Grid, Row }

@Composable
fun ListingCard(
    listing: Listing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    layout: ListingCardLayout = ListingCardLayout.Grid,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (layout == ListingCardLayout.Grid) {
            Column(Modifier.padding(MinkaSpacing.s8), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
                ListingPhoto(listing.cover, Modifier.fillMaxWidth().height(140.dp))
                ListingCardBody(listing, showCondition = false, Modifier.padding(MinkaSpacing.s8))
            }
        } else {
            Row(
                Modifier.heightIn(min = 112.dp).padding(MinkaSpacing.s8),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
            ) {
                ListingPhoto(listing.cover, Modifier.size(96.dp))
                ListingCardBody(listing, showCondition = true, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ListingCardBody(listing: Listing, showCondition: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
        Text(listing.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            listingMeta(
                district = listing.district,
                distanceKm = listing.distanceKm,
                condition = if (showCondition) stringResource(listing.condition.labelRes) else null,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
        ) {
            listing.wants.forEach { category ->
                MinkaBadge(
                    stringResource(
                        R.string.listing_wants,
                        stringResource(category.labelRes).lowercase(Locale.forLanguageTag("es")),
                    ),
                    tone = MinkaBadgeTone.Lavender,
                )
            }
        }
    }
}
