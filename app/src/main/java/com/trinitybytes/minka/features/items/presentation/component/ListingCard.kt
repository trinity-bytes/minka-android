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
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Listing
import java.text.NumberFormat

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
                ListingCardBody(listing, Modifier.padding(MinkaSpacing.s8))
            }
        } else {
            Row(
                Modifier.heightIn(min = 112.dp).padding(MinkaSpacing.s8),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
            ) {
                ListingPhoto(listing.cover, Modifier.size(96.dp))
                ListingCardBody(listing, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ListingCardBody(listing: Listing, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
        Text(listing.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val location = listing.distanceKm?.let {
            stringResource(R.string.listing_distance_km, listing.district, NumberFormat.getNumberInstance().format(it))
        } ?: listing.district
        Text(
            "$location · ${stringResource(conditionName(listing.condition))}",
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
        ) {
            listing.wants.forEach { category ->
                MinkaBadge(
                    stringResource(R.string.listing_wants, stringResource(categoryName(category))),
                    tone = MinkaBadgeTone.Lavender,
                )
            }
        }
    }
}

internal fun categoryName(category: Category): Int = when (category) {
    Category.HOME -> R.string.category_home
    Category.CLOTHING -> R.string.category_clothing
    Category.BOOKS -> R.string.category_books
    Category.TECHNOLOGY -> R.string.category_technology
    Category.PLANTS -> R.string.category_plants
    Category.SPORTS -> R.string.category_sports
    Category.TOOLS -> R.string.category_tools
    Category.SERVICES -> R.string.category_services
    Category.ANY -> R.string.category_any
}

private fun conditionName(condition: Condition): Int = when (condition) {
    Condition.NEW -> R.string.condition_new
    Condition.LIKE_NEW -> R.string.condition_like_new
    Condition.GOOD -> R.string.condition_good
    Condition.USED -> R.string.condition_used
}
