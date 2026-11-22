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
import com.trinitybytes.minka.navigation.ShellNavigator
import kotlinx.serialization.Serializable
import com.trinitybytes.minka.R
import com.trinitybytes.minka.navigation.ProfileTab

/** Public profile of another user (Figma 11.02), opened from a listing's owner card. */
@Serializable
data class OwnerProfileRoute(val ownerId: String) : NavKey

/** Profile destinations contributed to the signed-in shell. Placeholders until T7 builds flow 11. */
fun EntryProviderScope<NavKey>.profileEntries(@Suppress("UNUSED_PARAMETER") navigator: ShellNavigator) {
    entry<OwnerProfileRoute> { key ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(key.ownerId, style = MaterialTheme.typography.headlineMedium)
        }
    }
    entry<ProfileTab> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.nav_profile), style = MaterialTheme.typography.headlineMedium)
        }
    }
}
