package com.trinitybytes.minka.features.items.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.ListingStatus

private val ToolbarShape = RoundedCornerShape(40.dp)

/**
 * The owner's floating toolbar (Figma 4.03, 4.09): Pausar or Reanudar, and Borrar. "Editar" waits for a
 * use case that edits a published listing and "QR" for phase 4, so neither is drawn.
 */
@Composable
fun OwnerToolbar(
    status: ListingStatus,
    enabled: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = ToolbarShape, color = MaterialTheme.colorScheme.inverseSurface) {
        Row(Modifier.padding(MinkaSpacing.s8), horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
            if (status == ListingStatus.ACTIVE) {
                ToolbarItem(Icons.Rounded.Pause, stringResource(R.string.detail_action_pause), enabled, onPause)
            } else {
                ToolbarItem(Icons.Rounded.PlayArrow, stringResource(R.string.detail_action_resume), enabled, onResume)
            }
            ToolbarItem(Icons.Rounded.Delete, stringResource(R.string.detail_action_delete), enabled, onDelete)
        }
    }
}

@Composable
private fun ToolbarItem(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface)
    }
}

/** Figma 4.06: bones shaped like the photo and the text blocks, with the back arrow already usable. */
@Composable
fun DetailSkeleton(onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.detail_loading)
    Column(modifier.fillMaxSize().statusBarsPadding().semantics { contentDescription = loading }) {
        Box(Modifier.fillMaxWidth().height(320.dp)) {
            Bone(
                Modifier.fillMaxSize(),
                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            )
            if (onBack != null) {
                Box(Modifier.padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s12)) {
                    FloatingPhotoButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        description = stringResource(R.string.action_back),
                        onClick = onBack,
                    )
                }
            }
        }
        Column(Modifier.padding(MinkaSpacing.s16), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
            Bone(Modifier.width(300.dp).height(28.dp))
            Bone(Modifier.width(200.dp).height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
                Bone(Modifier.width(100.dp).height(32.dp))
                Bone(Modifier.width(88.dp).height(32.dp))
            }
            Bone(Modifier.fillMaxWidth().height(88.dp))
            Bone(Modifier.width(120.dp).height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
                Bone(Modifier.width(72.dp).height(32.dp))
                Bone(Modifier.width(120.dp).height(32.dp))
            }
        }
    }
}

@Composable
private fun Bone(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(12.dp)) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, shape))
}

/** Figma 4.05: the listing is gone; the way out is to look at similar objects nearby. */
@Composable
fun DetailUnavailable(
    onBack: (() -> Unit)?,
    onFindSimilar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().statusBarsPadding()) {
        if (onBack != null) {
            Box(Modifier.padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s12)) {
                FloatingPhotoButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = stringResource(R.string.action_back),
                    onClick = onBack,
                )
            }
        }
        Column(
            Modifier.align(Alignment.Center).padding(MinkaSpacing.s24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        ) {
            Icon(
                Icons.Rounded.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.detail_unavailable_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.detail_unavailable_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            MinkaButton(
                text = stringResource(R.string.detail_unavailable_action),
                onClick = onFindSimilar,
                size = MinkaButtonSize.Large,
            )
        }
    }
}
