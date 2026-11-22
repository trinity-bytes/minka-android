package com.trinitybytes.minka.features.home.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trinitybytes.minka.navigation.HomeTab

/** Home destinations contributed to the signed-in shell. */
fun EntryProviderScope<NavKey>.homeEntries() {
    entry<HomeTab> { HomeScreen() }
}
