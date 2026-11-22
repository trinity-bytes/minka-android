package com.trinitybytes.minka.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaAdaptiveBox
import com.trinitybytes.minka.core.designsystem.component.MinkaNavigationBar
import com.trinitybytes.minka.core.designsystem.component.MinkaNavigationItem
import com.trinitybytes.minka.core.designsystem.component.MinkaNavigationRail
import com.trinitybytes.minka.core.designsystem.component.isExpandedWidth
import com.trinitybytes.minka.features.items.presentation.home.homeEntries
import com.trinitybytes.minka.features.items.presentation.search.searchEntries
import com.trinitybytes.minka.features.profile.presentation.profileEntries
import kotlinx.serialization.Serializable

@Serializable
data object HomeTab : NavKey

@Serializable
data object SearchTab : NavKey

/** Monotonically increasing request, consumed by the future Buscar search field. */
val LocalSearchFocusRequest = staticCompositionLocalOf { 0 }

internal val LocalHomeExpanded = staticCompositionLocalOf { false }

@Serializable
data object ProfileTab : NavKey

internal fun <T> selectTab(current: T, selected: T, stacks: Map<T, MutableList<T>>): T {
    if (selected == current) {
        val stack = stacks.getValue(selected)
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
    }
    return selected
}

private enum class Tab(val root: NavKey, val label: Int, val icon: ImageVector) {
    Home(HomeTab, R.string.nav_home, Icons.Rounded.Home),
    Search(SearchTab, R.string.nav_search, Icons.Rounded.Search),
    Profile(ProfileTab, R.string.nav_profile, Icons.Rounded.Person),
}

/**
 * Signed-in navigation keeps a saveable back stack per tab. Reselecting the active tab pops it
 * to its root; switching tabs preserves each stack. All stacks stay decorated, including inactive
 * ones, so their saveable entry state and ViewModel stores are retained while another tab is shown.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MinkaShell(onPublish: () -> Unit = {}) {
    var selected by rememberSaveable { mutableStateOf(Tab.Home) }
    var searchFocusRequest by rememberSaveable { mutableIntStateOf(0) }
    var homeScrolled by rememberSaveable { mutableStateOf(false) }
    val home = rememberNavBackStack(HomeTab)
    val search = rememberNavBackStack(SearchTab)
    val profile = rememberNavBackStack(ProfileTab)
    val stacks = mapOf<NavKey, MutableList<NavKey>>(
        HomeTab to home,
        SearchTab to search,
        ProfileTab to profile,
    )
    val provider = entryProvider<NavKey> {
        homeEntries(
            onOpenSearch = { searchFocusRequest++; selected = Tab.Search },
            onOpenListing = {},
            onScrollChanged = { homeScrolled = it },
        )
        searchEntries()
        profileEntries()
    }
    val homeEntries = decorated(home, provider)
    val searchEntries = decorated(search, provider)
    val profileEntries = decorated(profile, provider)
    val entries = when (selected) {
        Tab.Home -> homeEntries
        Tab.Search -> searchEntries
        Tab.Profile -> profileEntries
    }
    val currentStack = stacks.getValue(selected.root)
    val back: () -> Unit = {
        if (currentStack.size > 1) {
            currentStack.removeAt(currentStack.lastIndex)
        } else {
            selected = Tab.Home
        }
    }
    BackHandler(enabled = currentStack.size > 1 || selected != Tab.Home, onBack = back)
    val items = Tab.entries.map { tab ->
        MinkaNavigationItem(
            label = stringResource(tab.label),
            icon = tab.icon,
            selected = selected == tab,
            onClick = {
                selectTab(selected.root, tab.root, stacks)
                selected = tab
            },
        )
    }
    MinkaAdaptiveBox(Modifier.fillMaxSize()) {
        val expanded = isExpandedWidth
        Row(Modifier.fillMaxSize()) {
            if (expanded) {
                MinkaNavigationRail(items = items, header = { PublishFab(onPublish) })
            }
            Scaffold(
                bottomBar = { if (!expanded) MinkaNavigationBar(items) },
                floatingActionButton = {
                    if (!expanded) PublishFab(onPublish, extended = selected == Tab.Home && !homeScrolled)
                },
            ) { padding ->
                CompositionLocalProvider(
                    LocalSearchFocusRequest provides searchFocusRequest,
                    LocalHomeExpanded provides expanded,
                ) {
                    NavDisplay(
                        entries = entries,
                        modifier = Modifier.padding(padding),
                        onBack = back,
                        sceneStrategies = listOf(rememberListDetailSceneStrategy<NavKey>()),
                    )
                }
            }
        }
    }
}

@Composable
private fun decorated(
    stack: List<NavKey>,
    provider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
    backStack = stack,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = provider,
)

@Composable
private fun PublishFab(onPublish: () -> Unit, extended: Boolean = false) {
    if (extended) {
        ExtendedFloatingActionButton(
            onClick = onPublish,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.nav_publish)) },
        )
        return
    }
    FloatingActionButton(
        onClick = onPublish,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.nav_publish))
    }
}
