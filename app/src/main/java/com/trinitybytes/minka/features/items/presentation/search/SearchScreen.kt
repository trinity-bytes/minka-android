package com.trinitybytes.minka.features.items.presentation.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.navigation.LocalOpenListingId
import com.trinitybytes.minka.navigation.LocalShellExpanded
import com.trinitybytes.minka.navigation.LocalSearchFocusRequest

/** What the screen can ask of its ViewModel; tests pass plain lambdas. */
internal class SearchActions(
    val onTextChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onSelectRecent: (String) -> Unit = {},
    val onSelectCategory: (Category) -> Unit = {},
    val onClearText: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onClearRecents: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRemoveFilters: () -> Unit = {},
    val onOpenFilters: () -> Unit = {},
    val filterSheet: FilterSheetActions = FilterSheetActions(),
)

/**
 * Buscar: recents and categories (Figma 3.01), suggestions (3.02), loading, empty and results (3.03-3.05) and
 * the filter sheet (3.07). Another screen that calls `openSearch` bumps [LocalSearchFocusRequest] and the
 * field takes focus (audit finding 16). Results open [onOpenListing]; beside the list at expanded widths the
 * shell shows that detail in the right pane.
 */
@Composable
fun SearchScreen(
    onOpenListing: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        SearchActions(
            onTextChange = viewModel::onTextChange,
            onSubmit = viewModel::onSubmit,
            onSelectRecent = viewModel::onSelectRecent,
            onSelectCategory = viewModel::onSelectCategory,
            onClearText = viewModel::onClearText,
            onExit = viewModel::onExit,
            onClearRecents = viewModel::clearRecents,
            onRetry = viewModel::retry,
            onRemoveFilters = viewModel::removeFilters,
            onOpenFilters = viewModel::openFilters,
            filterSheet = FilterSheetActions(
                onDistance = viewModel::setDraftDistance,
                onCondition = viewModel::setDraftCondition,
                onSort = viewModel::setDraftSort,
                onClear = viewModel::clearDraft,
                onApply = viewModel::applyFilters,
                onDismiss = viewModel::closeFilters,
            ),
        )
    }
    SearchContent(
        state = state,
        actions = actions,
        onOpenListing = onOpenListing,
        focusRequest = LocalSearchFocusRequest.current,
        expanded = LocalShellExpanded.current,
    )
}

@Composable
internal fun SearchContent(
    state: SearchUiState,
    actions: SearchActions,
    onOpenListing: (String) -> Unit,
    focusRequest: Int = 0,
    expanded: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var handledRequest by rememberSaveable { mutableIntStateOf(0) }
    val openListingId = LocalOpenListingId.current
    LaunchedEffect(focusRequest) {
        if (focusRequest > handledRequest) focusRequester.requestFocus()
        handledRequest = focusRequest
    }
    val active = state.stage != SearchStage.Browse || focused
    val leave = {
        focusManager.clearFocus()
        actions.onExit()
    }
    BackHandler(enabled = active && !(expanded && openListingId != null), onBack = leave)
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize()) {
        if (state.stage == SearchStage.Browse && !focused) {
            Text(
                stringResource(R.string.search_title),
                modifier = Modifier.padding(
                    start = MinkaSpacing.s16,
                    top = MinkaSpacing.s48,
                    bottom = MinkaSpacing.s16,
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        BuscarSearchField(
            value = state.text,
            onValueChange = actions.onTextChange,
            onSubmit = {
                focusManager.clearFocus()
                actions.onSubmit()
            },
            onClear = actions.onClearText,
            onBack = if (active) leave else null,
            focusRequester = focusRequester,
            onFocusChange = { focused = it },
            modifier = Modifier.padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s8),
        )
        Box(Modifier.fillMaxSize().padding(top = MinkaSpacing.s8)) {
            when (state.stage) {
                SearchStage.Browse -> BrowseContent(
                    recents = state.recents,
                    onSelectRecent = {
                        focusManager.clearFocus()
                        actions.onSelectRecent(it)
                    },
                    onClearRecents = actions.onClearRecents,
                    onSelectCategory = {
                        focusManager.clearFocus()
                        actions.onSelectCategory(it)
                    },
                    bottomPadding = imeBottom,
                )
                SearchStage.Suggest -> SuggestionsContent(
                    suggestions = state.suggestions,
                    onSelect = {
                        focusManager.clearFocus()
                        actions.onSelectRecent(it)
                    },
                    bottomPadding = imeBottom,
                )
                SearchStage.Results -> ResultsContent(
                    state = state,
                    actions = actions,
                    selectedId = openListingId.takeIf { expanded },
                    onOpenListing = onOpenListing,
                    bottomPadding = imeBottom,
                )
            }
        }
    }
    state.filterSheet?.let { FilterSheet(it, actions.filterSheet) }
}

@Composable
private fun ResultsContent(
    state: SearchUiState,
    actions: SearchActions,
    selectedId: String?,
    onOpenListing: (String) -> Unit,
    bottomPadding: Dp,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
    ) {
        FilterChips(state.filters, actions.onOpenFilters)
        when {
            state.loading -> ResultsSkeleton()
            state.error != null -> SearchErrorState(state.error, actions.onRetry)
            state.results.isEmpty() -> EmptyResults(
                text = state.text.trim(),
                hasFilters = state.filters.activeCount > 0,
                onRemoveFilters = actions.onRemoveFilters,
            )
            else -> ResultsList(state.results, selectedId, onOpenListing, bottomPadding)
        }
    }
}
