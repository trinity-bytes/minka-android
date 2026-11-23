package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.features.items.presentation.detail.ListingDetailRoute
import com.trinitybytes.minka.navigation.SearchTab
import com.trinitybytes.minka.navigation.ShellNavigator

/**
 * Search destinations contributed to the signed-in shell. Buscar is the list pane of the list-detail scene:
 * at 840 dp and up a result opens in the right pane, on compact widths it pushes the detail full screen.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.searchEntries(navigator: ShellNavigator) {
    entry<SearchTab>(metadata = ListDetailSceneStrategy.listPane()) {
        SearchScreen(onOpenListing = { id -> navigator.open(ListingDetailRoute(id)) })
    }
}
