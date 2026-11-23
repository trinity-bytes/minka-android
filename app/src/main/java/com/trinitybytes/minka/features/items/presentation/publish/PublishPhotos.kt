package com.trinitybytes.minka.features.items.presentation.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.trinitybytes.minka.features.items.presentation.component.rememberPhotoModel
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.MAX_PHOTOS

private val CoverSize = 120.dp
private val TileSize = 88.dp
private val RemoveTarget = 48.dp
private val RemoveDisc = 32.dp

/**
 * The photo strip of Figma 5.01, 5.03 and 5.05: a 120 dp cover, 88 dp tiles and the dashed add tile.
 * Every tile has a remove button (audit finding 17) and tapping it offers "Hacer portada" or "Quitar".
 * With [error] the add tile turns red (5.05).
 */
@Composable
internal fun PhotoStrip(
    photos: List<String>,
    canAdd: Boolean,
    error: Boolean,
    actions: PublishActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        verticalAlignment = Alignment.Bottom,
    ) {
        photos.forEachIndexed { index, photo ->
            PhotoTile(
                photo = photo,
                index = index,
                size = if (index == 0) CoverSize else TileSize,
                actions = actions,
            )
        }
        if (canAdd) {
            AddPhotoTile(
                size = if (photos.isEmpty()) CoverSize else TileSize,
                showLabel = photos.isEmpty(),
                error = error,
                onClick = actions.onAddPhotos,
            )
        }
        if (photos.isEmpty()) {
            repeat(2) { PlaceholderTile() }
        }
    }
}

/**
 * The photos of the expanded form (Figma expanded 5.03): the cover large, the rest in two columns,
 * and the add tile last.
 */
@Composable
internal fun PhotoGrid(
    photos: List<String>,
    canAdd: Boolean,
    error: Boolean,
    actions: PublishActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        if (photos.isEmpty()) {
            AddPhotoTile(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
                showLabel = true,
                error = error,
                onClick = actions.onAddPhotos,
            )
            return@Column
        }
        PhotoTile(
            photo = photos.first(),
            index = 0,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
            actions = actions,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            maxItemsInEachRow = 2,
        ) {
            photos.drop(1).forEachIndexed { offset, photo ->
                PhotoTile(
                    photo = photo,
                    index = offset + 1,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    actions = actions,
                )
            }
            if (canAdd) {
                AddPhotoTile(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    showLabel = false,
                    error = error,
                    onClick = actions.onAddPhotos,
                )
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photo: String,
    index: Int,
    actions: PublishActions,
    modifier: Modifier = Modifier,
    size: Dp? = null,
) {
    val isCover = index == 0
    val label = stringResource(
        if (isCover) R.string.publish_photo_label_cover else R.string.publish_photo_label,
        index + 1,
    )
    Box(
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                onClickLabel = stringResource(R.string.publish_photo_actions_title, index + 1),
                role = Role.Button,
                onClick = { actions.onPhotoTapped(index) },
            )
            .semantics { contentDescription = label },
    ) {
        AsyncImage(
            model = rememberPhotoModel(photo),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
        val removeLabel = stringResource(R.string.publish_photo_remove, index + 1)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(RemoveTarget)
                .clickable(role = Role.Button, onClick = { actions.onPhotoRemoved(index) })
                .semantics { contentDescription = removeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(RemoveDisc),
                shape = CircleShape,
                color = MinkaTheme.colors.raised,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.padding(MinkaSpacing.s8),
                )
            }
        }
        if (isCover) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = MinkaSpacing.s8),
                shape = CircleShape,
                color = MinkaTheme.colors.raised,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.publish_photo_cover),
                    modifier = Modifier.padding(horizontal = MinkaSpacing.s12, vertical = MinkaSpacing.s4),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun AddPhotoTile(
    showLabel: Boolean,
    error: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val outline = if (error) scheme.error else scheme.outline
    val shape = MaterialTheme.shapes.large
    val content = if (error) scheme.onErrorContainer else scheme.onSurfaceVariant
    val label = stringResource(if (showLabel) R.string.publish_photos_add else R.string.publish_photos_add_more)
    Column(
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(shape)
            .background(if (error) scheme.errorContainer else scheme.surfaceContainerLow)
            .dashedBorder(outline)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Rounded.AddAPhoto,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = content,
        )
        if (showLabel) {
            Text(
                text = stringResource(R.string.publish_photos_add),
                style = MaterialTheme.typography.labelMedium,
                color = content,
            )
        }
    }
}

/** The empty slots of Figma 5.01: only a hint of how many photos fit, hidden from accessibility. */
@Composable
private fun PlaceholderTile() {
    Box(
        modifier = Modifier
            .size(TileSize)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

private fun Modifier.dashedBorder(color: Color): Modifier = this.drawBehind {
    val stroke = 1.5.dp.toPx()
    val radius = 24.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

/** How many photos fit, for the helper text under the strip. */
internal const val PHOTO_LIMIT = MAX_PHOTOS
