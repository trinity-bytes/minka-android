package com.trinitybytes.minka.features.items.presentation.detail

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.features.profile.presentation.OwnerProfileRoute
import com.trinitybytes.minka.navigation.ShellNavigator
import kotlinx.serialization.Serializable

/** Listing detail (Figma 4.01–4.09) of [listingId]; the detail pane of Búsqueda's list-detail scene. */
@Serializable
data class ListingDetailRoute(val listingId: String) : NavKey

/** Detail destinations contributed to the signed-in shell (Figma flow 04). */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.detailEntries(navigator: ShellNavigator) {
    entry<ListingDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        ListingDetailScreen(
            route = key,
            onBack = navigator::back,
            onOpenOwner = { ownerId -> navigator.open(OwnerProfileRoute(ownerId)) },
            onFindSimilar = navigator::openSearch,
        )
    }
}
