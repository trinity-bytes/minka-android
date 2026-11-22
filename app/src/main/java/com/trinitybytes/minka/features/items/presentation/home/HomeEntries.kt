package com.trinitybytes.minka.features.items.presentation.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.navigation.HomeTab

/** Home destinations contributed to the signed-in shell. */
fun EntryProviderScope<NavKey>.homeEntries(
    onOpenSearch: () -> Unit,
    onOpenListing: (String) -> Unit,
    onScrollChanged: (Boolean) -> Unit,
) {
    entry<HomeTab> { HomeScreen(onOpenSearch, onOpenListing, onScrollChanged) }
}
