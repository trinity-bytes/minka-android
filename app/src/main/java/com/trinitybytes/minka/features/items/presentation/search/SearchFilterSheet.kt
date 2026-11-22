package com.trinitybytes.minka.features.items.presentation.search

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaBottomSheet
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.SearchSort
import com.trinitybytes.minka.features.items.presentation.component.formatDistance
import com.trinitybytes.minka.features.items.presentation.component.labelRes

/** What the sheet can change; each call edits the draft that "Ver N resultados" applies. */
internal class FilterSheetActions(
    val onDistance: (Double?) -> Unit = {},
    val onCondition: (Condition?) -> Unit = {},
    val onSort: (SearchSort) -> Unit = {},
    val onClear: () -> Unit = {},
    val onApply: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

private const val MIN_KM = 0.5f
private const val MAX_KM = 10f
private const val SLIDER_STEPS = 18

@get:StringRes
private val SearchSort.labelRes: Int
    get() = when (this) {
        SearchSort.NEAREST -> R.string.search_sort_nearest
        SearchSort.NEWEST -> R.string.search_sort_newest
    }

/**
 * Figma 3.07: distance slider (500 m to 10 km, the end meaning no limit), "Estado mínimo" and "Ordenar por".
 * "Reputación mínima" and "Mejor calificado" wait for phase 4. The apply button shows how many results remain.
 */
@Composable
internal fun FilterSheet(
    sheet: FilterSheetState,
    actions: FilterSheetActions,
) {
    MinkaBottomSheet(title = stringResource(R.string.search_sheet_title), onDismiss = actions.onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        ) {
            DistanceSection(sheet.draft.distanceKm, actions.onDistance)
            ConditionSection(sheet.draft.condition, actions.onCondition)
            SortSection(sheet.draft.sort, actions.onSort)
            Row(horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12)) {
                MinkaButton(
                    text = stringResource(R.string.search_sheet_clear),
                    onClick = actions.onClear,
                    modifier = Modifier.weight(1f),
                    style = MinkaButtonStyle.Outlined,
                    size = MinkaButtonSize.Large,
                )
                MinkaButton(
                    text = sheet.count?.let { pluralStringResource(R.plurals.search_sheet_apply_count, it, it) }
                        ?: stringResource(R.string.search_sheet_apply),
                    onClick = actions.onApply,
                    modifier = Modifier.weight(2f),
                    size = MinkaButtonSize.Large,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun DistanceSection(distanceKm: Double?, onDistance: (Double?) -> Unit) {
    var position by remember(distanceKm) { mutableFloatStateOf((distanceKm ?: MAX_KM.toDouble()).toFloat()) }
    val unlimited = position >= MAX_KM
    val sliderLabel = stringResource(R.string.search_sheet_distance_slider)
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        SectionLabel(
            if (unlimited) {
                stringResource(R.string.search_sheet_distance_unlimited)
            } else {
                stringResource(R.string.search_sheet_distance, formatDistance(position.toDouble()).orEmpty())
            },
        )
        Slider(
            value = position,
            onValueChange = { position = it },
            onValueChangeFinished = { onDistance(position.toDouble().takeUnless { position >= MAX_KM }) },
            modifier = Modifier.semantics { contentDescription = sliderLabel },
            valueRange = MIN_KM..MAX_KM,
            steps = SLIDER_STEPS,
            colors = SliderDefaults.colors(
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                activeTickColor = MaterialTheme.colorScheme.primary,
                inactiveTickColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDistance(MIN_KM.toDouble()).orEmpty(), style = MaterialTheme.typography.labelMedium)
            Text(formatDistance(MAX_KM.toDouble()).orEmpty(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ConditionSection(condition: Condition?, onCondition: (Condition?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        SectionLabel(stringResource(R.string.search_sheet_condition))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            Condition.entries.forEach { option ->
                MinkaFilterChip(
                    label = stringResource(option.labelRes),
                    selected = condition == option,
                    onClick = { onCondition(option.takeUnless { it == condition }) },
                )
            }
        }
    }
}

@Composable
private fun SortSection(sort: SearchSort, onSort: (SearchSort) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        SectionLabel(stringResource(R.string.search_sheet_sort))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            SearchSort.entries.forEach { option ->
                MinkaFilterChip(
                    label = stringResource(option.labelRes),
                    selected = sort == option,
                    onClick = { onSort(option) },
                )
            }
        }
    }
}
