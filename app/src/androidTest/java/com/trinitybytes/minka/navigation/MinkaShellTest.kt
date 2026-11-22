package com.trinitybytes.minka.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shell is composed with plain stand-ins for the tab screens: the real ones use Hilt view models, which
 * a plain Compose rule cannot provide. Publicar itself is covered by `PublishContentTest`.
 */
private val standIns: EntryProviderScope<NavKey>.() -> Unit = {
    entry<HomeTab> { Text("Inicio screen") }
    entry<SearchTab> { Text("Buscar screen") }
    entry<ProfileTab> { Text("Perfil screen") }
}

@RunWith(AndroidJUnit4::class)
class MinkaShellTest {
    @get:Rule val compose = createComposeRule()

    @Test fun destinationsSwitchAndPublishActionFires() {
        var published = false
        compose.setContent { MinkaTheme { MinkaShell({ published = true }, standIns) } }
        compose.onNodeWithText("Inicio screen").assertIsDisplayed()
        compose.onNode(hasText("Buscar") and hasClickAction()).performClick().assertIsSelected()
        compose.onNodeWithText("Buscar screen").assertIsDisplayed()
        compose.onNode(hasText("Perfil") and hasClickAction()).performClick().assertIsSelected()
        compose.onNodeWithText("Perfil screen").assertIsDisplayed()
        compose.onNodeWithContentDescription("Publicar").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(published) }
    }

    @Test fun selectedTabSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MinkaTheme { MinkaShell(tabEntries = standIns) } }
        compose.onNode(hasText("Buscar") and hasClickAction()).performClick().assertIsSelected()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasText("Buscar") and hasClickAction()).assertIsSelected()
        compose.onNodeWithText("Buscar screen").assertIsDisplayed()
    }
}
