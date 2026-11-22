package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.navigation.ShellNavigator
import com.trinitybytes.minka.R
import com.trinitybytes.minka.navigation.SearchTab

/** Search destinations contributed to the signed-in shell. */
fun EntryProviderScope<NavKey>.searchEntries(@Suppress("UNUSED_PARAMETER") navigator: ShellNavigator) {
    entry<SearchTab> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.headlineMedium)
        }
    }
}
