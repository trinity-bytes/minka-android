package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma Error state (`299:135`): a raised card with the [icon] on an error-container circle, the
 * [title], the [body] and an outlined action. While [actionLoading] the action shows the loading
 * indicator and ignores clicks.
 */
@Composable
fun MinkaErrorState(
    icon: ImageVector,
    title: String,
    body: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionLoading: Boolean = false,
) {
    Column(
        modifier = modifier
            .widthIn(max = ErrorStateMaxWidth)
            .fillMaxWidth()
            .background(MinkaTheme.colors.raised, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = MinkaSpacing.s24, vertical = MinkaSpacing.s32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
    ) {
        Box(
            modifier = Modifier
                .size(IllustrationSize)
                .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(IllustrationIconSize),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        MinkaButton(
            text = actionText,
            onClick = onAction,
            style = MinkaButtonStyle.Outlined,
            loading = actionLoading,
        )
    }
}

/**
 * Figma 1.22 "Sin conexión" for a step that has nothing to show without the network, such as a list
 * that could not load. Forms keep their content and use a snackbar instead. While [retrying]
 * "Reintentar" shows the loading indicator.
 */
@Composable
fun MinkaOfflineScreen(
    onBack: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retrying: Boolean = false,
) {
    MinkaErrorScreen(
        icon = Icons.Rounded.WifiOff,
        title = stringResource(R.string.offline_title),
        body = stringResource(R.string.offline_body),
        onBack = onBack,
        onRetry = onRetry,
        modifier = modifier,
        retrying = retrying,
    )
}

/** Like [MinkaOfflineScreen] for a list that failed to load for any other reason. */
@Composable
fun MinkaLoadErrorScreen(
    onBack: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retrying: Boolean = false,
) {
    MinkaErrorScreen(
        icon = Icons.Rounded.ErrorOutline,
        title = stringResource(R.string.load_error_title),
        body = stringResource(R.string.load_error_body),
        onBack = onBack,
        onRetry = onRetry,
        modifier = modifier,
        retrying = retrying,
    )
}

@Composable
private fun MinkaErrorScreen(
    icon: ImageVector,
    title: String,
    body: String,
    onBack: (() -> Unit)?,
    onRetry: () -> Unit,
    modifier: Modifier,
    retrying: Boolean,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MinkaTopAppBar(title = "", onBack = onBack) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = MinkaSpacing.s16, end = MinkaSpacing.s16, top = MinkaSpacing.s8, bottom = MinkaSpacing.s16),
            contentAlignment = Alignment.Center,
        ) {
            MinkaErrorState(
                icon = icon,
                title = title,
                body = body,
                actionText = stringResource(R.string.offline_retry),
                onAction = onRetry,
                actionLoading = retrying,
            )
        }
    }
}

private val ErrorStateMaxWidth = 340.dp
private val IllustrationSize = 96.dp
private val IllustrationIconSize = 40.dp

@Preview
@Composable
private fun MinkaOfflineScreenPreview() {
    MinkaTheme { MinkaOfflineScreen(onBack = {}, onRetry = {}) }
}
