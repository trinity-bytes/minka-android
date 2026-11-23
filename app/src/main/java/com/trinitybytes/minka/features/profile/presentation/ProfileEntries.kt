package com.trinitybytes.minka.features.profile.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.features.items.presentation.detail.ListingDetailRoute
import com.trinitybytes.minka.navigation.ProfileTab
import com.trinitybytes.minka.navigation.ShellNavigator
import kotlinx.serialization.Serializable

/** Public profile of another user (Figma 11.02), opened from a listing's owner card. */
@Serializable
data class OwnerProfileRoute(val ownerId: String) : NavKey

/** Mis publicaciones (Figma 11.05), opened from the own profile. */
@Serializable
data object MyListingsRoute : NavKey

/** Profile destinations contributed to the signed-in shell: own profile, Mis publicaciones and other owners. */
fun EntryProviderScope<NavKey>.profileEntries(navigator: ShellNavigator) {
    entry<ProfileTab> {
        ProfileScreen(onOpenMyListings = { navigator.open(MyListingsRoute) })
    }
    entry<MyListingsRoute> {
        MyListingsScreen(
            onBack = navigator::back,
            onPublish = { navigator.publish() },
            onOpenListing = { id -> navigator.open(ListingDetailRoute(id)) },
            onContinueDraft = { id -> navigator.publish(id) },
        )
    }
    entry<OwnerProfileRoute> { key ->
        OwnerProfileScreen(
            ownerId = key.ownerId,
            onBack = navigator::back,
            onOpenListing = { id -> navigator.open(ListingDetailRoute(id)) },
        )
    }
}
