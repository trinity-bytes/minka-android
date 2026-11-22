package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.R
import com.trinitybytes.minka.navigation.ProfileTab

/** Profile destinations contributed to the signed-in shell. */
fun EntryProviderScope<NavKey>.profileEntries() {
    entry<ProfileTab> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.nav_profile), style = MaterialTheme.typography.headlineMedium)
        }
    }
}
