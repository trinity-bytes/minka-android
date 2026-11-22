package com.trinitybytes.minka.features.items.presentation.publish

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.trinitybytes.minka.features.items.presentation.component.rememberPhotoModel
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaExpressiveShapes
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadingIndicator
import com.trinitybytes.minka.core.designsystem.component.MinkaSticker
import com.trinitybytes.minka.core.designsystem.component.MinkaStickerTone
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Listing

/** 5.09: the expressive loading indicator while the listing is sent; if it fails everything stays as a draft. */
@Composable
internal fun PublishingPage(photoCount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MinkaTheme.colors.mint, MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center,
        ) {
            MinkaLoadingIndicator(Modifier.size(64.dp))
        }
        Text(
            text = stringResource(R.string.publish_publishing_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = pluralStringResource(R.plurals.publish_publishing_body, photoCount, photoCount),
            modifier = Modifier.padding(horizontal = MinkaSpacing.s24),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 5.10, the hero moment of the product: the cover on a coral stage in an expressive blob, a check badge that
 * springs in and a tilted "Segunda vida" sticker. Eco-Puntos are hidden in this phase.
 */
@Composable
internal fun PublishedPage(listing: Listing, actions: PublishActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s24),
    ) {
        PhotoStage(cover = listing.cover)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            Text(
                text = stringResource(R.string.publish_done_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.publish_done_body, listing.title, listing.district),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            MinkaButton(
                text = stringResource(R.string.publish_done_view),
                onClick = { actions.onViewListing(listing.id) },
                modifier = Modifier.fillMaxWidth(),
                size = MinkaButtonSize.Large,
            )
            MinkaButton(
                text = stringResource(R.string.publish_done_share),
                onClick = actions.onShare,
                modifier = Modifier.fillMaxWidth(),
                style = MinkaButtonStyle.Outlined,
                leadingIcon = Icons.Rounded.Share,
                size = MinkaButtonSize.Large,
            )
            MinkaButton(
                text = stringResource(R.string.publish_done_another),
                onClick = actions.onPublishAnother,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MinkaButtonStyle.Text,
            )
        }
    }
}

@Composable
private fun PhotoStage(cover: String) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val pop by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "badge",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StageHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MinkaTheme.colors.coralContainer),
    ) {
        AsyncImage(
            model = rememberPhotoModel(cover),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(PHOTO_WIDTH_FRACTION)
                .fillMaxHeight(PHOTO_HEIGHT_FRACTION)
                .clip(MinkaExpressiveShapes.BlobA)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(MinkaSpacing.s16)
                .size(56.dp)
                .scale(pop)
                .background(MinkaTheme.colors.coral, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MinkaTheme.colors.onCoral,
            )
        }
        MinkaSticker(
            text = stringResource(R.string.publish_done_sticker),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = MinkaSpacing.s16, bottom = MinkaSpacing.s32),
            tone = MinkaStickerTone.Paper,
            rotation = -6f,
        )
    }
}

private val StageHeight = 360.dp
private const val PHOTO_WIDTH_FRACTION = 0.84f
private const val PHOTO_HEIGHT_FRACTION = 0.92f
