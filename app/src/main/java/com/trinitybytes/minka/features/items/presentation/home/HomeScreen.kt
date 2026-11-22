package com.trinitybytes.minka.features.items.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaAdaptiveBox
import com.trinitybytes.minka.core.designsystem.component.MinkaAvatar
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.component.MinkaLogo
import com.trinitybytes.minka.core.designsystem.component.MinkaMessageEffect
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaSticker
import com.trinitybytes.minka.core.designsystem.component.isExpandedWidth
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.presentation.component.ListingCard
import com.trinitybytes.minka.features.items.presentation.component.ItemCategories
import com.trinitybytes.minka.features.items.presentation.component.labelRes
import com.trinitybytes.minka.navigation.LocalHomeExpanded

/** Inicio loading, feed, refresh, empty and offline states (Figma 2.01–2.05). */
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenListing: (String) -> Unit,
    onScrollChanged: (Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onOpenSearch = onOpenSearch,
        onOpenListing = onOpenListing,
        onRefresh = viewModel::refresh,
        onSelectCategory = viewModel::selectCategory,
        onScrollChanged = onScrollChanged,
        onSelectAll = viewModel::selectAll,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: HomeUiState,
    onOpenSearch: () -> Unit,
    onOpenListing: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectCategory: (Category) -> Unit = {},
    onScrollChanged: (Boolean) -> Unit = {},
    onSelectAll: () -> Unit = {},
) {
    val gridState = rememberLazyGridState()
    val snackbar = remember { SnackbarHostState() }
    val offlineText = stringResource(R.string.feed_offline)
    val retryText = stringResource(R.string.feed_retry)
    MinkaMessageEffect(
        hostState = snackbar,
        message = if (state.offline && !state.refreshing) state.feed ?: state else null,
        text = offlineText,
        actionLabel = retryText,
        onShown = {},
        onAction = onRefresh,
    )
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0 }
            .collect { onScrollChanged(it) }
    }
    val nearby = state.feed?.nearby.orEmpty().filter {
        state.selectedCategory == null || it.category == state.selectedCategory
    }
    MinkaAdaptiveBox(Modifier.fillMaxSize()) {
        val expanded = isExpandedWidth || LocalHomeExpanded.current
        val columns = if (expanded) 4 else 2
        Box(Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MinkaSpacing.s16),
                    verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
                    horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
                ) {
                    if (state.loading) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "skeleton-header") { SkeletonHeader() }
                        items(columns * 2, key = { "skeleton-$it" }, contentType = { "skeleton" }) { SkeletonCard() }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                            HomeHeader(state, onOpenSearch, onSelectCategory, onSelectAll, onRefresh)
                        }
                        if (state.feed != null) {
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "title-nearby") {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.feed_nearby),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    MinkaButton(
                                        stringResource(R.string.feed_see_all),
                                        onOpenSearch,
                                        style = MinkaButtonStyle.Text,
                                    )
                                }
                            }
                            if (nearby.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                                    EmptyNearby(onOpenSearch)
                                }
                            } else {
                                listingItems(nearby, expanded, onOpenListing)
                            }
                        }
                    }
                }
            }
            MinkaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onOpenSearch: () -> Unit,
    onSelectCategory: (Category) -> Unit,
    onSelectAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MinkaLogo(Modifier.height(28.dp))
            Spacer(Modifier.weight(1f))
            MinkaAvatar(state.initials, Modifier.size(32.dp))
        }
        Column {
            val greeting = if (state.firstName.isBlank()) stringResource(R.string.feed_greeting)
            else stringResource(R.string.feed_greeting_name, state.firstName)
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
            val count = state.feed?.nearby?.size ?: 0
            if (count > 0) {
                Text(
                    if (state.district != null) {
                        stringResource(R.string.feed_nearby_count, count, state.district)
                    } else {
                        stringResource(R.string.feed_nearby_count_unknown, count)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (state.district != null) {
                Text(state.district, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MinkaShapes.full,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                Modifier.clickable(enabled = !state.offline, role = Role.Button, onClick = onOpenSearch)
                    .padding(horizontal = MinkaSpacing.s16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Text(stringResource(R.string.feed_search), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            MinkaFilterChip(
                label = stringResource(R.string.feed_all),
                selected = state.selectedCategory == null,
                onClick = onSelectAll,
            )
            ItemCategories.forEach { category ->
                MinkaFilterChip(
                    label = stringResource(category.labelRes),
                    selected = state.selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                )
            }
        }
        if (state.error && !state.offline) {
            Text(stringResource(R.string.feed_error), style = MaterialTheme.typography.bodyMedium)
            MinkaButton(stringResource(R.string.feed_retry), onRefresh, style = MinkaButtonStyle.Tonal)
        }
    }
}

@Composable
private fun SkeletonHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Bone(Modifier.width(96.dp).height(28.dp))
            Bone(Modifier.size(32.dp))
        }
        Bone(Modifier.width(200.dp).height(36.dp))
        Bone(Modifier.fillMaxWidth().height(56.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            listOf(56, 72, 64, 72).forEach { Bone(Modifier.width(it.dp).height(32.dp)) }
        }
        Bone(Modifier.width(140.dp).height(24.dp))
    }
}

@Composable
private fun Bone(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium))
}

@Composable
private fun SkeletonCard() {
    Column(Modifier.fillMaxWidth().height(226.dp), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        Bone(Modifier.fillMaxWidth().height(140.dp))
        Bone(Modifier.fillMaxWidth().height(20.dp))
        Bone(Modifier.fillMaxWidth(.7f).height(16.dp))
        Bone(Modifier.fillMaxWidth(.5f).height(24.dp))
    }
}

@Composable
private fun EmptyNearby(onOpenSearch: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = MinkaSpacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp).background(MinkaTheme.colors.skyContainer, MinkaShapes.bento)) {
            Box(
                Modifier.padding(MinkaSpacing.s20).size(64.dp).background(MinkaTheme.colors.sky, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Explore, contentDescription = null, tint = MinkaTheme.colors.onSky)
            }
            MinkaSticker(
                text = stringResource(R.string.feed_nearby_sticker),
                modifier = Modifier.align(Alignment.BottomStart).offset(x = 20.dp, y = (-20).dp),
                rotation = -4f,
            )
        }
        Text(stringResource(R.string.feed_empty_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.feed_empty_message), style = MaterialTheme.typography.bodyMedium)
        MinkaButton(stringResource(R.string.feed_expand_distance), onOpenSearch, style = MinkaButtonStyle.Tonal)
    }
}

private fun LazyGridScope.listingItems(listings: List<Listing>, expanded: Boolean, onOpenListing: (String) -> Unit) {
    items(listings, key = { it.id }, contentType = { "listing" }) { listing ->
        ListingCard(
            listing,
            onClick = { onOpenListing(listing.id) },
            modifier = if (expanded) Modifier.width(164.dp) else Modifier.fillMaxWidth(),
        )
    }
}
