package com.trinitybytes.minka.features.items.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaSticker
import com.trinitybytes.minka.core.designsystem.component.MinkaStickerTone
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.presentation.component.rememberPhotoModel

private val GalleryCorner = 24.dp
private val FloatingButtonSize = 40.dp
private const val PAUSED_TAG_TILT = -4f

/** The rounded photo area of Figma 4.01: a pager of photos with a position indicator and the paused tag. */
@Composable
fun DetailGallery(
    photos: List<String>,
    paused: Boolean,
    modifier: Modifier = Modifier,
    corners: RoundedCornerShape = RoundedCornerShape(bottomStart = GalleryCorner, bottomEnd = GalleryCorner),
    overlay: @Composable () -> Unit = {},
) {
    val pagerState = rememberPagerState { photos.size }
    Box(modifier.clip(corners).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = rememberPhotoModel(photos[page]),
                contentDescription = stringResource(R.string.detail_photo_position, page + 1, photos.size),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (paused) {
            MinkaSticker(
                text = stringResource(R.string.detail_tag_paused),
                modifier = Modifier.align(Alignment.BottomStart).padding(MinkaSpacing.s16),
                tone = MinkaStickerTone.Ink,
                rotation = PAUSED_TAG_TILT,
            )
        }
        if (photos.size > 1) {
            PagerDots(count = photos.size, selected = pagerState.currentPage, Modifier.align(Alignment.BottomCenter))
        }
        overlay()
    }
}

@Composable
private fun PagerDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(bottom = MinkaSpacing.s8)
            .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.7f), CircleShape)
            .padding(horizontal = MinkaSpacing.s8, vertical = MinkaSpacing.s4)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
    ) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                Modifier
                    .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                    .background(
                        MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = if (active) 1f else 0.6f),
                        CircleShape,
                    ),
            )
        }
    }
}

/** White circle with a 24 dp icon that floats over the photo (Figma 4.01 top actions). */
@Composable
fun FloatingPhotoButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(FloatingButtonSize),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(icon, contentDescription = description)
    }
}

/** Back, share and (for visitors) the "más" menu floating over the photo; [onBack] null hides the arrow. */
@Composable
fun GalleryActions(
    onBack: (() -> Unit)?,
    onShare: () -> Unit,
    onHide: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s12)) {
        if (onBack != null) {
            FloatingPhotoButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = stringResource(R.string.action_back),
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
        Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            FloatingPhotoButton(Icons.Rounded.Share, stringResource(R.string.detail_share), onShare)
            if (onHide != null) MoreMenu(onHide)
        }
    }
}

/** Figma 4.07: the "más" menu keeps only "Ocultar" in this phase (audit finding 13). */
@Composable
private fun MoreMenu(onHide: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FloatingPhotoButton(
            icon = Icons.Rounded.MoreVert,
            description = stringResource(R.string.detail_more),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_hide)) },
                leadingIcon = { Icon(Icons.Rounded.VisibilityOff, contentDescription = null) },
                onClick = {
                    expanded = false
                    onHide()
                },
            )
        }
    }
}
