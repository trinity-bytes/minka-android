package com.trinitybytes.minka.features.items.presentation.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.trinitybytes.minka.features.items.presentation.component.rememberPhotoModel
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.presentation.component.labelRes
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Step 3: the preview card and the checklist (Figma 5.08). Rows without a value show a dash and
 * "Agregar" instead of the green check, so an empty optional row does not read as required (audit
 * finding 20). With [showPreview] false the card is left out: the expanded layout puts it in its own pane.
 */
@Composable
internal fun ReviewPage(
    draft: ListingDraft,
    actions: PublishActions,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        StepProgress(PublishPage.Review)
        PageTitle(stringResource(R.string.publish_review_title))
        if (showPreview) PreviewCard(draft)
        Checklist(draft, actions)
    }
}

@Composable
private fun Checklist(draft: ListingDraft, actions: PublishActions) {
    val category = draft.category?.let { stringResource(it.labelRes) }.orEmpty()
    val wants = draft.wants.map { stringResource(it.labelRes) }.joinToString(", ")
    Column {
        CheckRow(
            title = stringResource(R.string.publish_review_photos),
            summary = photosSummary(draft.photos.size),
            done = draft.photos.isNotEmpty(),
            onAction = { actions.onEdit(PublishPage.Basics) },
        )
        CheckRow(
            title = stringResource(R.string.publish_review_title_category),
            summary = listOf(draft.title, category).filter { it.isNotBlank() }.joinToString(" · "),
            done = draft.title.isNotBlank() && draft.category != null,
            onAction = { actions.onEdit(PublishPage.Basics) },
        )
        CheckRow(
            title = stringResource(R.string.publish_review_wants),
            summary = wants,
            done = draft.wants.isNotEmpty(),
            onAction = { actions.onEdit(PublishPage.Wants) },
        )
        CheckRow(
            title = stringResource(R.string.publish_review_description),
            summary = draft.description,
            done = draft.description != null,
            onAction = actions.onDetailsTapped,
        )
        CheckRow(
            title = stringResource(R.string.publish_review_value),
            summary = draft.estimatedValue?.let { stringResource(R.string.publish_money, formatSoles(it)) },
            done = draft.estimatedValue != null,
            onAction = actions.onDetailsTapped,
        )
    }
}

@Composable
private fun CheckRow(title: String, summary: String?, done: Boolean, onAction: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val actionText = stringResource(if (done) R.string.publish_review_edit else R.string.publish_review_add)
    val actionDescription = stringResource(
        if (done) R.string.publish_review_edit_cd else R.string.publish_review_add_cd,
        title,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Remove,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (done) scheme.primary else scheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            Text(
                text = summary?.takeIf { it.isNotBlank() } ?: stringResource(R.string.publish_review_empty),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        MinkaButton(
            text = actionText,
            onClick = onAction,
            modifier = Modifier.semantics { contentDescription = actionDescription },
            style = MinkaButtonStyle.Text,
        )
    }
}

/** The compact preview of Figma 5.08: cover, title, "district · category · estado" and the first "Busca:" chip. */
@Composable
internal fun PreviewCard(draft: ListingDraft, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val facts = listOfNotNull(
        draft.district.takeIf { it.isNotBlank() },
        draft.category?.let { stringResource(it.labelRes) },
        draft.condition?.let { stringResource(it.labelRes) },
    ).joinToString(" · ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MinkaTheme.colors.raised, MaterialTheme.shapes.extraLarge)
            .padding(MinkaSpacing.s8),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = rememberPhotoModel(draft.photos.firstOrNull()),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.large)
                .background(scheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
            Text(
                text = draft.title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                maxLines = 2,
            )
            Text(text = facts, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            draft.wants.firstOrNull()?.let { WantChip(it) }
        }
    }
}

/** "Busca: libros" on the lavender container reserved for what an owner wants (audit E3). */
@Composable
private fun WantChip(category: Category) {
    Text(
        text = stringResource(R.string.publish_review_wants_chip, stringResource(category.labelRes).lowercase()),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .padding(horizontal = MinkaSpacing.s12, vertical = MinkaSpacing.s4),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/** The large preview of the expanded review (Figma expanded 5.08), left of the checklist. */
@Composable
internal fun PreviewPane(draft: ListingDraft, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        Text(
            text = stringResource(R.string.publish_review_preview_caption),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MinkaTheme.colors.raised),
        ) {
            AsyncImage(
                model = rememberPhotoModel(draft.photos.firstOrNull()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .background(scheme.surfaceContainerHigh),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                Text(text = draft.title, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
                Text(
                    text = stringResource(R.string.publish_review_preview_info, draft.district),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                    verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                ) {
                    draft.condition?.let { InfoChip(stringResource(it.labelRes)) }
                    draft.category?.let { InfoChip(stringResource(it.labelRes)) }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(horizontal = MinkaSpacing.s16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Centavos as soles without trailing zeros: 12000 is "120", 4550 is "45,50". */
internal fun formatSoles(centavos: Long): String {
    val soles = BigDecimal(centavos).movePointLeft(2)
    val text = if (centavos % 100 == 0L) {
        soles.toBigInteger().toString()
    } else {
        soles.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
    }
    return text.replace('.', ',')
}
