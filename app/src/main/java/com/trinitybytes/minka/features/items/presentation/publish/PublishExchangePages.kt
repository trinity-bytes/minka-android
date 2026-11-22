package com.trinitybytes.minka.features.items.presentation.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.component.MinkaListGroup
import com.trinitybytes.minka.core.designsystem.component.MinkaListItem
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.DraftError
import com.trinitybytes.minka.features.items.presentation.component.WantedCategories
import com.trinitybytes.minka.features.items.presentation.component.labelRes

/** Step 2, first page: what the owner would like in exchange (Figma 5.06; Eco-Puntos are hidden in this phase). */
@Composable
internal fun WantsPage(state: PublishUiState, actions: PublishActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        StepProgress(PublishPage.Wants)
        PageTitle(stringResource(R.string.publish_wants_title))
        Text(
            text = stringResource(R.string.publish_wants_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            WantedCategories.forEach { category ->
                MinkaFilterChip(
                    label = stringResource(category.labelRes),
                    selected = category in state.draft.wants,
                    onClick = { actions.onWantToggled(category) },
                )
            }
        }
        if (DraftError.EXCHANGE_REQUIRED in state.errors) {
            FieldError(stringResource(R.string.publish_error_exchange))
        }
    }
}

/**
 * Step 2, second page: district and meeting point without a map, plus the two switches (Figma 5.07,
 * audit findings 15 and 18). Both rows are whole-row targets without chevrons.
 */
@Composable
internal fun LocationPage(state: PublishUiState, actions: PublishActions, modifier: Modifier = Modifier) {
    val draft = state.draft
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        StepProgress(PublishPage.Location)
        PageTitle(stringResource(R.string.publish_location_title))
        MinkaListGroup {
            MinkaListItem(
                headline = if (draft.district.isBlank()) {
                    stringResource(R.string.publish_district_row_empty)
                } else {
                    stringResource(R.string.publish_district_row, draft.district)
                },
                supporting = stringResource(R.string.publish_district_row_hint),
                leadingIcon = Icons.Rounded.LocationOn,
                onClick = actions.onDistrictRowTapped,
            )
            MinkaListItem(
                headline = stringResource(R.string.publish_meeting_row),
                supporting = draft.meetingPoint ?: stringResource(R.string.publish_meeting_row_empty),
                leadingIcon = Icons.Rounded.Storefront,
                onClick = actions.onMeetingPointRowTapped,
            )
        }
        if (DraftError.DISTRICT_REQUIRED in state.errors) {
            FieldError(stringResource(R.string.publish_error_district))
        }
        SwitchRow(
            title = stringResource(R.string.publish_hide_location),
            supporting = stringResource(R.string.publish_hide_location_hint),
            checked = draft.hideExactLocation,
            onCheckedChange = actions.onHideLocationChange,
        )
        SwitchRow(
            title = stringResource(R.string.publish_home_delivery),
            supporting = stringResource(R.string.publish_home_delivery_hint),
            checked = draft.homeDelivery,
            onCheckedChange = actions.onHomeDeliveryChange,
        )
    }
}

/** A title, one line of help and a switch; the whole row toggles it. */
@Composable
private fun SwitchRow(
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = MinkaSpacing.s4),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
