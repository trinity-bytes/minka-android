package com.trinitybytes.minka.features.items.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.navigation.ShellNavigator
import kotlinx.serialization.Serializable

/** Listing detail (Figma 4.01–4.09) of [listingId]; the detail pane of Búsqueda's list-detail scene. */
@Serializable
data class ListingDetailRoute(val listingId: String) : NavKey

/** Detail destinations contributed to the signed-in shell. Placeholder until T5 builds flow 04. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.detailEntries(@Suppress("UNUSED_PARAMETER") navigator: ShellNavigator) {
    entry<ListingDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(key.listingId, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
