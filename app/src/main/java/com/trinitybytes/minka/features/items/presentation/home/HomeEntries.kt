package com.trinitybytes.minka.features.items.presentation.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.features.items.presentation.detail.ListingDetailRoute
import com.trinitybytes.minka.navigation.ShellNavigator
import com.trinitybytes.minka.navigation.HomeTab

/** Home destinations contributed to the signed-in shell. */
fun EntryProviderScope<NavKey>.homeEntries(
    navigator: ShellNavigator,
    onScrollChanged: (Boolean) -> Unit,
) {
    entry<HomeTab> {
        HomeScreen(
            onOpenSearch = navigator::openSearch,
            onOpenListing = { id -> navigator.open(ListingDetailRoute(id)) },
            onScrollChanged = onScrollChanged,
        )
    }
}
