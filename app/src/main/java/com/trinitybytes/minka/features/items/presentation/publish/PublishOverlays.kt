package com.trinitybytes.minka.features.items.presentation.publish

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaBottomSheet
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaDialog
import com.trinitybytes.minka.core.designsystem.component.MinkaListGroup
import com.trinitybytes.minka.core.designsystem.component.MinkaListItem
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadingIndicator
import com.trinitybytes.minka.core.designsystem.component.MinkaTextField
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.MAX_PHOTOS
import com.trinitybytes.minka.features.items.presentation.component.ItemCategories
import com.trinitybytes.minka.features.items.presentation.component.icon
import com.trinitybytes.minka.features.items.presentation.component.labelRes

/** The sheet or dialog [PublishUiState.overlay] asks for. */
@Composable
internal fun PublishOverlays(state: PublishUiState, actions: PublishActions) {
    when (val overlay = state.overlay) {
        null -> Unit
        PublishOverlay.PhotoSource -> PhotoSourceSheet(
            room = MAX_PHOTOS - state.draft.photos.size,
            actions = actions,
        )
        is PublishOverlay.PhotoActions -> PhotoActionsSheet(overlay.index, actions)
        PublishOverlay.Details -> DetailsSheet(state, actions)
        PublishOverlay.Category -> CategorySheet(state, actions)
        PublishOverlay.District -> DistrictSheet(state, actions)
        PublishOverlay.MeetingPoint -> MeetingPointDialog(state, actions)
        PublishOverlay.ExitConfirm -> ExitDialog(actions)
    }
}

/** 5.02: "Tomar foto" or "Elegir de la galería"; neither needs a runtime permission (audit finding 6). */
@Composable
private fun PhotoSourceSheet(room: Int, actions: PublishActions) {
    MinkaBottomSheet(
        title = stringResource(R.string.publish_source_title),
        onDismiss = actions.onDismissOverlay,
    ) {
        MinkaListGroup {
            MinkaListItem(
                headline = stringResource(R.string.publish_source_camera),
                supporting = stringResource(R.string.publish_source_camera_hint),
                leadingIcon = Icons.Rounded.PhotoCamera,
                onClick = actions.onTakePhoto,
            )
            MinkaListItem(
                headline = stringResource(R.string.publish_source_gallery),
                supporting = stringResource(R.string.publish_source_gallery_hint, room),
                leadingIcon = Icons.Rounded.Image,
                onClick = actions.onPickFromGallery,
            )
        }
    }
}

/** 5.03: tapping a photo offers "Hacer portada" (not for the cover) or "Quitar" (audit finding 17). */
@Composable
private fun PhotoActionsSheet(index: Int, actions: PublishActions) {
    MinkaBottomSheet(
        title = stringResource(R.string.publish_photo_actions_title, index + 1),
        onDismiss = actions.onDismissOverlay,
    ) {
        MinkaListGroup {
            if (index > 0) {
                MinkaListItem(
                    headline = stringResource(R.string.publish_photo_make_cover),
                    leadingIcon = Icons.Rounded.Star,
                    onClick = { actions.onMakeCover(index) },
                )
            }
            MinkaListItem(
                headline = stringResource(R.string.publish_photo_remove_action),
                leadingIcon = Icons.Rounded.Delete,
                onClick = { actions.onPhotoRemoved(index) },
            )
        }
    }
}

/** 5.04: description and estimated value, both optional. "Estado" already lives in step 1. */
@Composable
private fun DetailsSheet(state: PublishUiState, actions: PublishActions) {
    var description by rememberSaveable { mutableStateOf(state.draft.description.orEmpty()) }
    var value by rememberSaveable {
        mutableStateOf(state.draft.estimatedValue?.let(::formatSoles).orEmpty())
    }
    MinkaBottomSheet(
        title = stringResource(R.string.publish_details_title),
        onDismiss = actions.onDismissOverlay,
    ) {
        MinkaTextField(
            value = description,
            onValueChange = { description = it },
            label = stringResource(R.string.publish_details_description),
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        MinkaTextField(
            value = value,
            onValueChange = { value = it.filter { char -> char.isDigit() || char == ',' || char == '.' } },
            label = stringResource(R.string.publish_details_value),
            supportingText = stringResource(R.string.publish_details_value_hint),
            visualTransformation = SolesPrefix,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        )
        MinkaButton(
            text = stringResource(R.string.publish_details_done),
            onClick = { actions.onDetailsDone(description, value) },
            modifier = Modifier.fillMaxWidth(),
            size = MinkaButtonSize.Large,
        )
    }
}

/** "S/ 120": shows the currency before the digits without making it part of the value. */
private object SolesPrefix : VisualTransformation {
    private const val PREFIX = "S/ "

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + PREFIX.length

            override fun transformedToOriginal(offset: Int) = (offset - PREFIX.length).coerceAtLeast(0)
        }
        return TransformedText(AnnotatedString(PREFIX) + text, mapping)
    }
}

/** The "Categoría" picker: the six categories of Figma 3.01. */
@Composable
private fun CategorySheet(state: PublishUiState, actions: PublishActions) {
    MinkaBottomSheet(
        title = stringResource(R.string.publish_category_sheet_title),
        onDismiss = actions.onDismissOverlay,
    ) {
        MinkaListGroup {
            ItemCategories.forEach { category ->
                MinkaListItem(
                    headline = stringResource(category.labelRes),
                    leadingIcon = category.icon,
                    trailingIcon = Icons.Rounded.Check.takeIf { category == state.draft.category },
                    onClick = { actions.onCategoryChosen(category) },
                )
            }
        }
    }
}

/** The district picker behind the 5.07 district row. */
@Composable
private fun DistrictSheet(state: PublishUiState, actions: PublishActions) {
    MinkaBottomSheet(
        title = stringResource(R.string.publish_district_sheet_title),
        onDismiss = actions.onDismissOverlay,
    ) {
        val districts = state.districts
        if (districts == null) {
            MinkaLoadingIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@MinkaBottomSheet
        }
        Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            MinkaListGroup {
                districts.forEach { district ->
                    MinkaListItem(
                        headline = district.name,
                        trailingIcon = Icons.Rounded.Check.takeIf { district.name == state.draft.district },
                        onClick = { actions.onDistrictChosen(district.name) },
                    )
                }
            }
        }
    }
}

/** The meeting point, a free text; an empty one clears it. */
@Composable
private fun MeetingPointDialog(state: PublishUiState, actions: PublishActions) {
    var text by rememberSaveable { mutableStateOf(state.draft.meetingPoint.orEmpty()) }
    MinkaDialog(
        title = stringResource(R.string.publish_meeting_title),
        text = "",
        confirmText = stringResource(R.string.publish_details_done),
        onConfirm = { actions.onMeetingPointDone(text) },
        onDismiss = actions.onDismissOverlay,
        dismissText = stringResource(R.string.publish_meeting_cancel),
        textContent = {
            MinkaTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.publish_meeting_label),
                placeholder = stringResource(R.string.publish_meeting_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
    )
}

/** 5.11: closing with unsaved data offers "Guardar" or "Descartar". */
@Composable
private fun ExitDialog(actions: PublishActions) {
    MinkaDialog(
        title = stringResource(R.string.publish_exit_title),
        text = stringResource(R.string.publish_exit_body),
        confirmText = stringResource(R.string.publish_exit_save),
        onConfirm = actions.onExitSave,
        onDismiss = actions.onDismissOverlay,
        dismissText = stringResource(R.string.publish_exit_discard),
        icon = Icons.Rounded.Save,
        iconTint = MaterialTheme.colorScheme.primary,
        confirmStyle = MinkaButtonStyle.Filled,
        onDismissClick = actions.onExitDiscard,
    )
}
