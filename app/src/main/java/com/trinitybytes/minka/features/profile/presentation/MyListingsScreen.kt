package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaAdaptiveBox
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaDialog
import com.trinitybytes.minka.core.designsystem.component.MinkaErrorState
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadingIndicator
import com.trinitybytes.minka.core.designsystem.component.MinkaMessageEffect
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.ObserveAsEvents
import com.trinitybytes.minka.core.designsystem.component.isExpandedWidth
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.navigation.LocalHomeExpanded
import kotlinx.coroutines.launch

private val ListPaneWidth = 360.dp
private val FabClearance = 88.dp

/** What the user can do on 11.05; the tests pass only the ones they check. */
internal class MyListingsActions(
    val onBack: () -> Unit = {},
    val onPublish: () -> Unit = {},
    val onSelectFilter: (MyListingsFilter) -> Unit = {},
    val onOpenListing: (String) -> Unit = {},
    val onContinueDraft: (String) -> Unit = {},
    val onSelect: (String) -> Unit = {},
    val onPause: (String) -> Unit = {},
    val onResume: (String) -> Unit = {},
    val onRequestDelete: (String) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {},
    val onRefresh: () -> Unit = {},
)

/**
 * Mis publicaciones (Figma 11.05, 11.06 and the expanded 11.05): status chips Activas, Pausadas and
 * Borradores, a row per listing with an overflow menu, "Continuar" on drafts, the 4.08 confirmation to
 * delete, and the pause snackbar with "Deshacer" (audit P1 to P4). From 840 dp the list sits beside a pane
 * that manages the chosen listing. [onOpenListing] opens its detail, [onContinueDraft] resumes Publicar.
 */
@Composable
fun MyListingsScreen(
    onBack: () -> Unit,
    onPublish: () -> Unit,
    onOpenListing: (String) -> Unit,
    onContinueDraft: (String) -> Unit,
    viewModel: MyListingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pausedText = stringResource(R.string.my_listings_paused_message)
    val undoText = stringResource(R.string.my_listings_undo)
    val resumedText = stringResource(R.string.my_listings_resumed_message)
    val deletedText = stringResource(R.string.my_listings_deleted_message)
    val failedText = stringResource(R.string.my_listings_failed_message)
    ObserveAsEvents(viewModel.events) { event ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            when (event) {
                is MyListingsEvent.Paused -> {
                    val result = snackbar.showSnackbar(pausedText, actionLabel = undoText)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoPause(event.listingId)
                }
                MyListingsEvent.Resumed -> snackbar.showSnackbar(resumedText)
                MyListingsEvent.Deleted -> snackbar.showSnackbar(deletedText)
                MyListingsEvent.Failed -> snackbar.showSnackbar(failedText)
            }
        }
    }
    MyListingsContent(
        state = state,
        snackbar = snackbar,
        actions = MyListingsActions(
            onBack = onBack,
            onPublish = onPublish,
            onSelectFilter = viewModel::selectFilter,
            onOpenListing = onOpenListing,
            onContinueDraft = onContinueDraft,
            onSelect = viewModel::select,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onRequestDelete = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onCancelDelete = viewModel::cancelDelete,
            onRefresh = viewModel::refresh,
        ),
    )
}

@Composable
internal fun MyListingsContent(
    state: MyListingsUiState,
    snackbar: SnackbarHostState,
    actions: MyListingsActions,
) {
    MinkaMessageEffect(
        hostState = snackbar,
        message = if (state.offline && !state.error) true else null,
        text = stringResource(R.string.my_listings_offline),
        onShown = {},
        actionLabel = stringResource(R.string.my_listings_retry),
        onAction = actions.onRefresh,
    )
    Scaffold(
        topBar = {
            MinkaTopAppBar(
                title = stringResource(R.string.my_listings_title),
                onBack = actions.onBack,
                actions = {
                    IconButton(onClick = actions.onPublish) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.my_listings_publish))
                    }
                },
            )
        },
        snackbarHost = { MinkaSnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        MinkaAdaptiveBox(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            val expanded = isExpandedWidth || LocalHomeExpanded.current
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MinkaLoadingIndicator()
                }
                state.error -> LoadFailure(state.offline, actions.onRefresh)
                expanded -> Row(Modifier.fillMaxSize()) {
                    ListPane(state, actions, expanded = true, Modifier.width(ListPaneWidth).fillMaxHeight())
                    ListingManagePane(
                        listing = selectedListing(state),
                        onOpen = { selectedListing(state)?.let { actions.onOpenListing(it.id) } },
                        onPause = { selectedListing(state)?.let { actions.onPause(it.id) } },
                        onResume = { selectedListing(state)?.let { actions.onResume(it.id) } },
                        onDelete = { selectedListing(state)?.let { actions.onRequestDelete(it.id) } },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = MinkaSpacing.s8, end = MinkaSpacing.s16, bottom = MinkaSpacing.s16),
                    )
                }
                else -> ListPane(state, actions, expanded = false, Modifier.fillMaxSize())
            }
        }
    }
    if (state.pendingDelete != null) {
        MinkaDialog(
            title = stringResource(R.string.my_listings_delete_title),
            text = stringResource(R.string.my_listings_delete_body),
            confirmText = stringResource(R.string.my_listings_delete),
            onConfirm = actions.onConfirmDelete,
            onDismiss = actions.onCancelDelete,
            dismissText = stringResource(R.string.my_listings_delete_cancel),
        )
    }
}

/** The listing the expanded pane shows: the chosen one if it is still listed, otherwise the first of the filter. */
private fun selectedListing(state: MyListingsUiState): Listing? {
    val shown = when (state.filter) {
        MyListingsFilter.ACTIVE -> state.active
        MyListingsFilter.PAUSED -> state.paused
        MyListingsFilter.DRAFTS -> emptyList()
    }
    return shown.firstOrNull { it.id == state.selectedId } ?: shown.firstOrNull()
}

@Composable
private fun ListPane(
    state: MyListingsUiState,
    actions: MyListingsActions,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        FilterRow(state.filter, actions.onSelectFilter)
        val selectedId = selectedListing(state)?.id
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MinkaSpacing.s16,
                end = MinkaSpacing.s16,
                top = MinkaSpacing.s4,
                bottom = if (expanded) MinkaSpacing.s16 else FabClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
        ) {
            when (state.filter) {
                MyListingsFilter.DRAFTS -> {
                    if (state.drafts.isEmpty()) item { EmptyList(state.filter, actions.onPublish) }
                    items(state.drafts, key = { it.id }) { saved ->
                        DraftRow(
                            saved = saved,
                            onContinue = { actions.onContinueDraft(saved.id) },
                            onDelete = { actions.onRequestDelete(saved.id) },
                        )
                    }
                }
                else -> {
                    val listings = if (state.filter == MyListingsFilter.ACTIVE) state.active else state.paused
                    if (listings.isEmpty()) item { EmptyList(state.filter, actions.onPublish) }
                    items(listings, key = { it.id }) { listing ->
                        ListingRow(
                            listing = listing,
                            selected = expanded && listing.id == selectedId,
                            onClick = {
                                if (expanded) actions.onSelect(listing.id) else actions.onOpenListing(listing.id)
                            },
                            onPause = { actions.onPause(listing.id) },
                            onResume = { actions.onResume(listing.id) },
                            onDelete = { actions.onRequestDelete(listing.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: MyListingsFilter, onSelect: (MyListingsFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s8),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
    ) {
        MyListingsFilter.entries.forEach { filter ->
            MinkaFilterChip(
                label = stringResource(filter.labelRes()),
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

private fun MyListingsFilter.labelRes(): Int = when (this) {
    MyListingsFilter.ACTIVE -> R.string.my_listings_filter_active
    MyListingsFilter.PAUSED -> R.string.my_listings_filter_paused
    MyListingsFilter.DRAFTS -> R.string.my_listings_filter_drafts
}

@Composable
private fun EmptyList(filter: MyListingsFilter, onPublish: () -> Unit) {
    val (title, body) = when (filter) {
        MyListingsFilter.ACTIVE ->
            R.string.my_listings_empty_active_title to R.string.my_listings_empty_active_body
        MyListingsFilter.PAUSED ->
            R.string.my_listings_empty_paused_title to R.string.my_listings_empty_paused_body
        MyListingsFilter.DRAFTS ->
            R.string.my_listings_empty_drafts_title to R.string.my_listings_empty_drafts_body
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = MinkaSpacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (filter == MyListingsFilter.ACTIVE) {
            MinkaButton(stringResource(R.string.my_listings_publish), onPublish, style = MinkaButtonStyle.Tonal)
        }
    }
}

@Composable
private fun LoadFailure(offline: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(MinkaSpacing.s16), contentAlignment = Alignment.Center) {
        MinkaErrorState(
            icon = if (offline) Icons.Rounded.WifiOff else Icons.Rounded.ErrorOutline,
            title = stringResource(if (offline) R.string.offline_title else R.string.load_error_title),
            body = stringResource(if (offline) R.string.offline_body else R.string.load_error_body),
            actionText = stringResource(R.string.offline_retry),
            onAction = onRetry,
        )
    }
}
