package com.trinitybytes.minka.features.items.presentation.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.component.MinkaTextField
import com.trinitybytes.minka.core.designsystem.component.MinkaWavyProgress
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.DraftError
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.presentation.component.labelRes

/** The wavy progress and "Paso N de 3 · …" label that open every page of the form (Figma 5.01–5.08). */
@Composable
internal fun StepProgress(page: PublishPage, modifier: Modifier = Modifier) {
    val name = stringResource(
        when (page.step) {
            1 -> R.string.publish_step_basics
            2 -> R.string.publish_step_exchange
            else -> R.string.publish_step_review
        },
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        MinkaWavyProgress(progress = page.step.toFloat() / PUBLISH_STEPS)
        Text(
            text = stringResource(R.string.publish_step, page.step, PUBLISH_STEPS, name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The headline of a page, announced as a heading. */
@Composable
internal fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** The red icon and message under a field (Figma 5.05). */
@Composable
internal fun FieldError(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { error(text) },
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Error,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

/**
 * "Guardar borrador" next to the main action, on every step (audit finding 19). At large font scales
 * the two buttons stack so neither label wraps.
 */
@Composable
internal fun PublishActionBar(
    primaryText: String,
    onPrimary: () -> Unit,
    onSaveDraft: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
) {
    val stacked = LocalDensity.current.fontScale >= StackedFontScale
    val save: @Composable (Modifier) -> Unit = {
        MinkaButton(
            text = stringResource(R.string.publish_save_draft),
            onClick = onSaveDraft,
            modifier = it,
            style = MinkaButtonStyle.Outlined,
            size = MinkaButtonSize.Large,
            loading = isSaving,
        )
    }
    val primary: @Composable (Modifier) -> Unit = {
        MinkaButton(
            text = primaryText,
            onClick = onPrimary,
            modifier = it,
            size = MinkaButtonSize.Large,
        )
    }
    if (stacked) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            primary(Modifier.fillMaxWidth())
            save(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            save(Modifier.weight(1f))
            primary(Modifier.weight(1f))
        }
    }
}

private const val StackedFontScale = 1.3f

/**
 * Title, category and "Estado" with "Agregar más detalles" (Figma 5.01, 5.03, 5.05). [DraftError]s
 * show under their field as soon as "Continuar" found them.
 */
@Composable
internal fun BasicsFields(
    draft: ListingDraft,
    errors: Set<DraftError>,
    actions: PublishActions,
    modifier: Modifier = Modifier,
) {
    val titleMissing = DraftError.TITLE_REQUIRED in errors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        MinkaTextField(
            value = draft.title,
            onValueChange = actions.onTitleChange,
            label = stringResource(R.string.publish_title_label),
            placeholder = stringResource(R.string.publish_title_placeholder),
            supportingText = stringResource(R.string.publish_error_title).takeIf { titleMissing },
            isError = titleMissing,
        )
        CategoryField(
            categoryLabel = draft.category?.let { stringResource(it.labelRes) }.orEmpty(),
            isError = DraftError.CATEGORY_REQUIRED in errors,
            onClick = actions.onCategoryRowTapped,
        )
        ConditionChips(
            selected = draft.condition,
            isError = DraftError.CONDITION_REQUIRED in errors,
            onChosen = actions.onConditionChosen,
        )
        MinkaButton(
            text = stringResource(R.string.publish_more_details),
            onClick = actions.onDetailsTapped,
            style = MinkaButtonStyle.Text,
            leadingIcon = Icons.Rounded.Add,
        )
    }
}

/**
 * The "Categoría" field looks like the other fields but opens a picker: an overlay takes the tap, and
 * it is announced as one button that carries the label, the value and the error.
 */
@Composable
private fun CategoryField(categoryLabel: String, isError: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.publish_category_label)
    val message = stringResource(R.string.publish_error_category)
    val description = listOf(label, categoryLabel).filter { it.isNotEmpty() }.joinToString(", ")
    Box {
        MinkaTextField(
            value = categoryLabel,
            onValueChange = {},
            label = label,
            modifier = Modifier.clearAndSetSemantics {},
            placeholder = stringResource(R.string.publish_category_placeholder),
            supportingText = message.takeIf { isError },
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = if (isError) Icons.Rounded.Error else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                )
            },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = description
                    if (isError) error(message)
                },
        )
    }
}

@Composable
private fun ConditionChips(selected: Condition?, isError: Boolean, onChosen: (Condition) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        Text(
            text = stringResource(R.string.publish_condition_label),
            style = MaterialTheme.typography.titleSmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            Condition.entries.forEach { condition ->
                MinkaFilterChip(
                    label = stringResource(condition.labelRes),
                    selected = condition == selected,
                    onClick = { onChosen(condition) },
                )
            }
        }
        if (isError) FieldError(stringResource(R.string.publish_error_condition))
    }
}

/** The helper line under the photos, or the cover hint once there are photos (Figma 5.01 vs 5.03). */
@Composable
internal fun PhotoHint(hasPhotos: Boolean, error: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        if (error) {
            FieldError(stringResource(R.string.publish_error_photo))
        } else {
            Text(
                text = if (hasPhotos) {
                    stringResource(R.string.publish_photos_cover_hint)
                } else {
                    stringResource(R.string.publish_photos_hint, PHOTO_LIMIT)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "* Obligatorio". */
@Composable
internal fun RequiredNote(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.publish_required_note),
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Photos of a listing as the review words them: "2 fotos · portada elegida". */
@Composable
internal fun photosSummary(count: Int): String =
    pluralStringResource(R.plurals.publish_review_photos_summary, count, count)
