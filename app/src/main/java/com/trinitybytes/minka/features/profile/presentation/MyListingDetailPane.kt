package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaBadge
import com.trinitybytes.minka.core.designsystem.component.MinkaBadgeTone
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.presentation.component.ListingPhoto
import com.trinitybytes.minka.features.items.presentation.component.labelRes
import com.trinitybytes.minka.features.items.presentation.component.listingMeta
import java.util.Locale

private val GalleryHeight = 200.dp

/**
 * The right pane of the expanded 11.05: the chosen listing is managed without leaving the list. The
 * views, proposals and saved counters and the proposals card wait for the exchanges phase and are hidden;
 * the floating toolbar of the mockup is a row of buttons, and "Editar" is hidden with the rest.
 */
@Composable
internal fun ListingManagePane(
    listing: Listing?,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        if (listing == null) {
            Box(Modifier.fillMaxSize().padding(MinkaSpacing.s24), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.my_listings_pick_one),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Surface
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(MinkaSpacing.s16),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        ) {
            ListingPhoto(listing.cover, Modifier.fillMaxWidth().height(GalleryHeight).clip(MinkaShapes.bento))
            Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
                Text(listing.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = listingMeta(listing.district, null, stringResource(listing.condition.labelRes)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(text = listingSubtitle(listing), style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                MinkaBadge(stringResource(listing.category.labelRes), tone = MinkaBadgeTone.Neutral)
                listing.wants.forEach { category ->
                    MinkaBadge(
                        text = stringResource(
                            R.string.listing_wants,
                            stringResource(category.labelRes).lowercase(Locale.forLanguageTag("es")),
                        ),
                        tone = MinkaBadgeTone.Lavender,
                    )
                }
            }
            listing.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                MinkaButton(stringResource(R.string.my_listings_view), onOpen, style = MinkaButtonStyle.Tonal)
                if (listing.status == ListingStatus.PAUSED) {
                    MinkaButton(
                        text = stringResource(R.string.my_listings_resume),
                        onClick = onResume,
                        style = MinkaButtonStyle.Outlined,
                        leadingIcon = Icons.Rounded.PlayArrow,
                    )
                } else {
                    MinkaButton(
                        text = stringResource(R.string.my_listings_pause),
                        onClick = onPause,
                        style = MinkaButtonStyle.Outlined,
                        leadingIcon = Icons.Rounded.Pause,
                    )
                }
                MinkaButton(
                    text = stringResource(R.string.my_listings_delete),
                    onClick = onDelete,
                    style = MinkaButtonStyle.Text,
                    leadingIcon = Icons.Rounded.Delete,
                )
            }
        }
    }
}
