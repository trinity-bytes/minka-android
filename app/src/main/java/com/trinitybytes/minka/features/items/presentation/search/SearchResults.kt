package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaErrorState
import com.trinitybytes.minka.core.designsystem.component.MinkaExpressiveShapes
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.presentation.component.ListingCard
import com.trinitybytes.minka.features.items.presentation.component.ListingCardLayout
import com.trinitybytes.minka.features.items.presentation.component.formatDistance
import com.trinitybytes.minka.features.items.presentation.component.labelRes

/**
 * Filters button and the chips of the applied filters (Figma 3.03-3.05). Every chip opens the sheet, where
 * the filters change; the category chosen in 3.01 shows here as a chip too.
 */
@Composable
internal fun FilterChips(
    filters: SearchFilters,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MinkaSpacing.s16),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
    ) {
        FiltersButton(filters.activeCount, onOpenFilters)
        filters.distanceKm?.let { km ->
            val text = formatDistance(km).orEmpty()
            MinkaFilterChip(
                label = stringResource(R.string.search_filter_distance, text),
                selected = false,
                onClick = onOpenFilters,
            )
        }
        filters.condition?.let { condition ->
            MinkaFilterChip(stringResource(condition.labelRes), selected = false, onClick = onOpenFilters)
        }
        filters.category?.let { category ->
            MinkaFilterChip(stringResource(category.labelRes), selected = false, onClick = onOpenFilters)
        }
    }
}

/** The ink "Filtros" pill of Figma 3.03: the tune icon and, once something is applied, "· N". */
@Composable
private fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val label = if (activeCount > 0) {
        stringResource(R.string.search_filters_count, activeCount)
    } else {
        stringResource(R.string.search_filters)
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.height(32.dp).semantics { role = Role.Button },
        shape = MinkaShapes.full,
        color = scheme.inverseSurface,
        contentColor = scheme.inverseOnSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MinkaSpacing.s12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Figma 3.05: the count and the row cards. [selectedId] is the result whose detail is open beside the list. */
@Composable
internal fun ResultsList(
    results: List<Listing>,
    selectedId: String?,
    onOpenListing: (String) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = MinkaSpacing.s16, end = MinkaSpacing.s16, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
    ) {
        item(key = "count", contentType = "count") {
            Text(
                pluralStringResource(R.plurals.search_result_count, results.size, results.size),
                modifier = Modifier.padding(vertical = MinkaSpacing.s4),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(results, key = { it.id }, contentType = { "listing" }) { listing ->
            ListingCard(
                listing = listing,
                onClick = { onOpenListing(listing.id) },
                modifier = Modifier.fillMaxWidth(),
                layout = ListingCardLayout.Row,
                selected = listing.id == selectedId,
            )
        }
    }
}

/** Figma 3.03: six placeholder rows with the photo and three text lines. */
@Composable
internal fun ResultsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MinkaSpacing.s16),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
    ) {
        repeat(SKELETON_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
                Bone(Modifier.size(72.dp), MaterialTheme.shapes.large)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
                    Bone(Modifier.fillMaxWidth().height(16.dp))
                    Bone(Modifier.fillMaxWidth(0.7f).height(12.dp))
                    Bone(Modifier.fillMaxWidth(0.35f).height(20.dp))
                }
            }
        }
    }
}

private const val SKELETON_ROWS = 6

@Composable
private fun Bone(modifier: Modifier, shape: Shape = MinkaShapes.full) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, shape))
}

/**
 * Figma 3.04. The primary action is "Quitar filtros" (audit finding 12) and only appears when a filter is
 * applied; "Guardar búsqueda" waits for saved searches.
 */
@Composable
internal fun EmptyResults(
    text: String,
    hasFilters: Boolean,
    onRemoveFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = MinkaSpacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(MinkaTheme.colors.coralContainer, MinkaExpressiveShapes.Cookie12),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MinkaTheme.colors.onCoralContainer,
            )
        }
        Text(
            if (text.isBlank()) {
                stringResource(R.string.search_empty_title_filters)
            } else {
                stringResource(R.string.search_empty_title, text)
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(if (hasFilters) R.string.search_empty_message else R.string.search_empty_message_plain),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (hasFilters) MinkaButton(stringResource(R.string.search_remove_filters), onRemoveFilters)
    }
}

@Composable
internal fun SearchErrorState(error: SearchError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(MinkaSpacing.s16), contentAlignment = Alignment.Center) {
        MinkaErrorState(
            icon = Icons.Rounded.WifiOff,
            title = stringResource(R.string.search_error_title),
            body = stringResource(
                if (error == SearchError.Offline) R.string.search_error_offline else R.string.search_error_unexpected,
            ),
            actionText = stringResource(R.string.search_retry),
            onAction = onRetry,
        )
    }
}
