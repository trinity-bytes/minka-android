package com.trinitybytes.minka.features.items.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaDialog
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadErrorScreen
import com.trinitybytes.minka.core.designsystem.component.MinkaOfflineScreen
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.ObserveAsEvents
import com.trinitybytes.minka.core.designsystem.component.ExpandedWidthBreakpoint
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import java.time.Instant
import kotlinx.coroutines.launch

/** Width of the information column that still gets a photo pane of its own beside it (Figma expanded 4.01). */
private val TwoPaneMinWidth = 600.dp
private val MediaPaneWidth = 320.dp
private val ToolbarClearance = 100.dp

/**
 * Listing detail (Figma flow 04): the 4.06 skeleton, 4.01 for someone else's listing (4.07 menu, hide with
 * undo), 4.03 and 4.09 for the owner's (pause, resume, 4.08 delete confirmation) and 4.05 when the listing
 * is gone. It shows two panes, photo 320 dp plus information, when it has room, and drops its own back
 * arrow when Búsqueda shows it as the detail pane of an expanded window.
 */
@Composable
fun ListingDetailScreen(
    route: ListingDetailRoute,
    onBack: () -> Unit,
    onOpenOwner: (String) -> Unit,
    onFindSimilar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListingDetailViewModel = hiltViewModel<ListingDetailViewModel, ListingDetailViewModel.Factory>(
        creationCallback = { it.create(route) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val leave by rememberUpdatedState(onBack)
    val pausedText = stringResource(R.string.detail_message_paused)
    val resumedText = stringResource(R.string.detail_message_resumed)
    val hiddenText = stringResource(R.string.detail_message_hidden)
    val failedText = stringResource(R.string.detail_message_failed)
    val undoText = stringResource(R.string.detail_undo)
    ObserveAsEvents(viewModel.events) { event ->
        scope.launch {
            when (event) {
                DetailEvent.Paused -> {
                    val result = snackbarHostState.showSnackbar(pausedText, undoText, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.resume()
                }
                DetailEvent.Resumed -> snackbarHostState.showSnackbar(resumedText)
                DetailEvent.Hidden -> {
                    val result = snackbarHostState.showSnackbar(hiddenText, undoText, duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoHide() else leave()
                }
                DetailEvent.Deleted -> leave()
                DetailEvent.ActionFailed -> snackbarHostState.showSnackbar(failedText)
            }
        }
    }
    val actions = DetailActions(
        onBack = onBack,
        onRetry = viewModel::retry,
        onShare = { state.listing?.let { shareListing(context, it) } },
        onHide = viewModel::hide,
        onOpenOwner = onOpenOwner,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
        onFindSimilar = onFindSimilar,
    )
    ListingDetailContent(
        state = state,
        actions = actions,
        snackbarHostState = snackbarHostState,
        now = remember { Instant.now() },
        modifier = modifier,
    )
}

/** Stateless detail screen, so UI tests can drive every state without a ViewModel. */
@Composable
fun ListingDetailContent(
    state: DetailUiState,
    actions: DetailActions,
    snackbarHostState: SnackbarHostState,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val windowWidth = LocalWindowInfo.current.containerDpSize.width
        val asPane = windowWidth >= ExpandedWidthBreakpoint && maxWidth < TwoPaneMinWidth
        val onBack = actions.onBack.takeUnless { asPane }
        when (val phase = state.phase) {
            DetailPhase.Loading -> DetailSkeleton(onBack)
            DetailPhase.Unavailable -> DetailUnavailable(onBack, actions.onFindSimilar)
            is DetailPhase.Failed -> if (phase.offline) {
                MinkaOfflineScreen(onBack = onBack, onRetry = actions.onRetry)
            } else {
                MinkaLoadErrorScreen(onBack = onBack, onRetry = actions.onRetry)
            }
            DetailPhase.Loaded -> state.listing?.let { listing ->
                LoadedDetail(listing, state, actions, onBack, now, twoPane = maxWidth >= TwoPaneMinWidth)
            }
        }
        MinkaSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (state.isOwner && state.phase == DetailPhase.Loaded) ToolbarClearance else 0.dp),
        )
        if (state.confirmingDelete) DeleteDialog(actions)
    }
}

@Composable
private fun LoadedDetail(
    listing: Listing,
    state: DetailUiState,
    actions: DetailActions,
    onBack: (() -> Unit)?,
    now: Instant,
    twoPane: Boolean,
) {
    val paused = listing.status == ListingStatus.PAUSED
    val galleryActions: @Composable () -> Unit = {
        GalleryActions(
            onBack = onBack,
            onShare = actions.onShare,
            onHide = actions.onHide.takeUnless { state.isOwner },
        )
    }
    Box(Modifier.fillMaxSize()) {
        if (twoPane) {
            Row(Modifier.fillMaxSize().statusBarsPadding().padding(MinkaSpacing.s8)) {
                DetailGallery(
                    photos = listing.photos,
                    paused = paused,
                    modifier = Modifier.width(MediaPaneWidth).fillMaxHeight(),
                    corners = RoundedCornerShape(24.dp),
                    overlay = galleryActions,
                )
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    DetailInfo(
                        listing, state.isOwner, state.offline, now, actions.onOpenOwner,
                        Modifier.padding(MinkaSpacing.s16),
                    )
                    if (state.isOwner) Spacer(Modifier.height(ToolbarClearance))
                }
            }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
                DetailGallery(
                    photos = listing.photos,
                    paused = paused,
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    overlay = galleryActions,
                )
                DetailInfo(
                    listing, state.isOwner, state.offline, now, actions.onOpenOwner,
                    Modifier.padding(MinkaSpacing.s16),
                )
                if (state.isOwner) Spacer(Modifier.height(ToolbarClearance))
            }
        }
        if (state.isOwner) {
            OwnerToolbar(
                status = listing.status,
                enabled = !state.acting,
                onPause = actions.onPause,
                onResume = actions.onResume,
                onDelete = actions.onDelete,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = MinkaSpacing.s16),
            )
        }
    }
}

/** Figma 4.08: confirmation that also offers pausing, the reversible alternative (audit finding 9). */
@Composable
private fun DeleteDialog(actions: DetailActions) {
    MinkaDialog(
        title = stringResource(R.string.detail_delete_title),
        text = stringResource(R.string.detail_delete_body),
        confirmText = stringResource(R.string.detail_delete_confirm),
        onConfirm = actions.onConfirmDelete,
        onDismiss = actions.onDismissDelete,
        dismissText = stringResource(R.string.detail_delete_cancel),
        confirmStyle = MinkaButtonStyle.Destructive,
        textContent = {
            Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12)) {
                Text(
                    stringResource(R.string.detail_delete_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MinkaButton(
                    text = stringResource(R.string.detail_delete_pause_instead),
                    onClick = actions.onPause,
                    modifier = Modifier.fillMaxWidth(),
                    style = MinkaButtonStyle.Tonal,
                )
            }
        },
    )
}

/** Opens the system share sheet with plain text only; nothing is exported and no URI is granted. */
private fun shareListing(context: Context, listing: Listing) {
    val text = context.getString(R.string.detail_share_text, listing.title, listing.district)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.detail_share_title))
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        // No app can take plain text: sharing is a convenience, so there is nothing else to do.
    }
}
