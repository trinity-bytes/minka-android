package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaBadge
import com.trinitybytes.minka.core.designsystem.component.MinkaBadgeTone
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.SavedDraft
import com.trinitybytes.minka.features.items.presentation.component.ListingPhoto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PhotoSize = 96.dp
private val RowMinHeight = 112.dp
private val OverflowSize = 48.dp
private val PausedDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))

private class MenuEntry(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean,
    val onClick: () -> Unit,
)

/**
 * A published listing of 11.05: photo, title, age or pause date, status badge and the overflow menu
 * with Pausar o Reanudar and Borrar (audit P2). "Editar" is hidden: no edit flow exists in phase 3.
 * [selected] outlines the row that the expanded detail pane manages.
 */
@Composable
internal fun ListingRow(
    listing: Listing,
    selected: Boolean,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paused = listing.status == ListingStatus.PAUSED
    val entries = listOf(
        if (paused) {
            MenuEntry(stringResource(R.string.my_listings_resume), Icons.Rounded.PlayArrow, false, onResume)
        } else {
            MenuEntry(stringResource(R.string.my_listings_pause), Icons.Rounded.Pause, false, onPause)
        },
        MenuEntry(stringResource(R.string.my_listings_delete), Icons.Rounded.Delete, true, onDelete),
    )
    Box(modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = RowMinHeight)
                    .padding(
                        start = MinkaSpacing.s8,
                        top = MinkaSpacing.s8,
                        bottom = MinkaSpacing.s8,
                        end = OverflowSize,
                    ),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListingPhoto(listing.cover, Modifier.size(PhotoSize))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(text = listingSubtitle(listing), style = MaterialTheme.typography.bodySmall)
                    MinkaBadge(
                        text = stringResource(
                            if (paused) R.string.my_listings_badge_paused else R.string.my_listings_badge_active,
                        ),
                        tone = if (paused) MinkaBadgeTone.Neutral else MinkaBadgeTone.Brand,
                    )
                }
            }
        }
        OverflowMenu(listing.title, entries, Modifier.align(Alignment.CenterEnd))
    }
}

/**
 * A draft of 11.05 with "Continuar" (audit P1). It can be deleted from the overflow menu, which the
 * mockup does not draw; that is the only way to discard a draft.
 */
@Composable
internal fun DraftRow(
    saved: SavedDraft,
    onContinue: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = saved.draft.title.ifBlank { stringResource(R.string.my_listings_untitled) }
    val entries = listOf(
        MenuEntry(stringResource(R.string.my_listings_delete), Icons.Rounded.Delete, true, onDelete),
    )
    Box(modifier) {
        Surface(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = RowMinHeight)
                    .padding(MinkaSpacing.s8),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
            ) {
                val cover = saved.draft.photos.firstOrNull()
                if (cover != null) {
                    ListingPhoto(cover, Modifier.size(PhotoSize))
                } else {
                    Box(
                        Modifier
                            .size(PhotoSize)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(end = OverflowSize - MinkaSpacing.s8),
                    verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.my_listings_draft_step, draftStep(saved.draft)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        MinkaBadge(
                            text = stringResource(R.string.my_listings_badge_draft),
                            tone = MinkaBadgeTone.Neutral,
                        )
                        MinkaButton(
                            text = stringResource(R.string.my_listings_continue),
                            onClick = onContinue,
                            style = MinkaButtonStyle.Tonal,
                        )
                    }
                }
            }
        }
        OverflowMenu(title, entries, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun OverflowMenu(title: String, entries: List<MenuEntry>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.my_listings_more_for, title))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                val scheme = MaterialTheme.colorScheme
                val tint = if (entry.destructive) scheme.error else scheme.onSurface
                DropdownMenuItem(
                    text = { Text(entry.label, color = tint) },
                    leadingIcon = { Icon(entry.icon, contentDescription = null, tint = tint) },
                    onClick = {
                        expanded = false
                        entry.onClick()
                    },
                )
            }
        }
    }
}

/** "Publicado hace 2 días" for an active listing, "Pausada desde el 18 oct" for a paused one. */
@Composable
internal fun listingSubtitle(listing: Listing): String {
    val now = remember { Instant.now() }
    return when (listing.status) {
        ListingStatus.ACTIVE -> stringResource(
            R.string.my_listings_published_ago,
            ageText(listingAge(listing.createdAt, now)),
        )
        ListingStatus.PAUSED -> stringResource(
            R.string.my_listings_paused_since,
            PausedDate.format(listing.updatedAt.atZone(ZoneId.systemDefault())),
        )
    }
}

@Composable
private fun ageText(age: ListingAge): String = when (age.unit) {
    ListingAgeUnit.NOW -> stringResource(R.string.age_now)
    ListingAgeUnit.HOURS -> pluralStringResource(R.plurals.age_hours, age.count, age.count)
    ListingAgeUnit.DAYS -> pluralStringResource(R.plurals.age_days, age.count, age.count)
    ListingAgeUnit.MONTHS -> pluralStringResource(R.plurals.age_months, age.count, age.count)
}
