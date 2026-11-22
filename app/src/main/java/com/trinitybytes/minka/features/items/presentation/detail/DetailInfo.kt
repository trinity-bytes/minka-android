package com.trinitybytes.minka.features.items.presentation.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaAvatar
import com.trinitybytes.minka.core.designsystem.component.initialsOf
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import com.trinitybytes.minka.features.items.presentation.component.labelRes
import com.trinitybytes.minka.features.items.presentation.component.listingMeta
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SpanishLocale = Locale.forLanguageTag("es")
private const val CENTAVOS = 100

/**
 * Everything under the photo of Figma 4.01 and 4.03: title and meta line, condition and category chips,
 * the owner card (visitors), "Busca a cambio", the description and the estimated value. The proposal
 * button, rating, seal, stats and received proposals belong to later phases and are not drawn.
 */
@Composable
fun DetailInfo(
    listing: Listing,
    isOwner: Boolean,
    offline: Boolean,
    now: Instant,
    onOpenOwner: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        if (offline) {
            Text(
                stringResource(R.string.detail_offline_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
            Text(listing.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                metaLine(listing, now),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            DetailChip(stringResource(listing.condition.labelRes))
            DetailChip(stringResource(listing.category.labelRes))
        }
        if (!isOwner) {
            OwnerCard(listing.owner, onOpenOwner)
            if (listing.status == ListingStatus.PAUSED) PausedNotice()
        }
        Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            Text(stringResource(R.string.detail_wants_title), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                listing.wants.forEach { DetailChip(stringResource(it.labelRes)) }
            }
        }
        listing.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        listing.estimatedValue?.let { EstimatedValue(it) }
    }
}

@Composable
private fun metaLine(listing: Listing, now: Instant): String {
    val place = listingMeta(listing.district, listing.distanceKm)
    val age = when (val age = listingAge(listing.createdAt, now)) {
        ListingAge.Today -> stringResource(R.string.detail_age_today)
        ListingAge.Yesterday -> stringResource(R.string.detail_age_yesterday)
        is ListingAge.Days -> pluralStringResource(R.plurals.detail_age_days, age.count, age.count)
        is ListingAge.Weeks -> pluralStringResource(R.plurals.detail_age_weeks, age.count, age.count)
        is ListingAge.Months -> pluralStringResource(R.plurals.detail_age_months, age.count, age.count)
    }
    return stringResource(R.string.detail_meta, place, age)
}

/** Static outlined pill (Figma Chip, 32 dp): a label, not a control. */
@Composable
fun DetailChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 32.dp),
        shape = MinkaShapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s4),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** The owner card of Figma 4.01: avatar, name, "Miembro desde …" when known, and "Ver perfil". */
@Composable
private fun OwnerCard(owner: OwnerSummary, onOpenOwner: (String) -> Unit) {
    Surface(
        onClick = { onOpenOwner(owner.id) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(MinkaSpacing.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
        ) {
            MinkaAvatar(initialsOf(owner.name))
            Column(Modifier.weight(1f)) {
                Text(
                    owner.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ownerSubtitle(owner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = MinkaShapes.full,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    stringResource(R.string.detail_view_profile),
                    modifier = Modifier.padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s8),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ownerSubtitle(owner: OwnerSummary): String {
    val since = owner.memberSince ?: return owner.district
    val month = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", SpanishLocale)
        .format(since.atZone(ZoneId.systemDefault()))
    return stringResource(R.string.detail_owner_member_since, month)
}

/** Figma 4.02, adapted: a paused listing seen by someone else (no "Avisarme" action before phase 4). */
@Composable
private fun PausedNotice() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(MinkaSpacing.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(stringResource(R.string.detail_paused_notice), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EstimatedValue(centavos: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.detail_estimated_value), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(MinkaSpacing.s8))
        Text(stringResource(R.string.detail_money, formatSoles(centavos)), style = MaterialTheme.typography.bodyMedium)
    }
}

/** 12000 centavos is "120"; cents are shown only when there are some ("120,50"). */
fun formatSoles(centavos: Long): String {
    val format = NumberFormat.getNumberInstance(SpanishLocale)
    format.maximumFractionDigits = if (centavos % CENTAVOS == 0L) 0 else 2
    format.minimumFractionDigits = format.maximumFractionDigits
    return format.format(centavos / CENTAVOS.toDouble())
}
