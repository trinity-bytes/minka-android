package com.trinitybytes.minka.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import kotlinx.serialization.Serializable
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shell is composed with plain stand-ins for the tab screens: the real ones use Hilt view models, which
 * a plain Compose rule cannot provide. Publicar itself is covered by `PublishContentTest`.
 */
@Serializable
private data object PushedKey : NavKey

private val standIns: EntryProviderScope<NavKey>.(ShellNavigator) -> Unit = { navigator ->
    entry<HomeTab> {
        Column {
            Text("Inicio screen")
            Button(onClick = { navigator.open(PushedKey) }) { Text("Abrir detalle") }
        }
    }
    entry<PushedKey> { Text("Detalle screen") }
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
        compose.onAllNodes(hasText("Publicar"), useUnmergedTree = true).assertCountEquals(0)
        compose.onNode(hasText("Perfil") and hasClickAction()).performClick().assertIsSelected()
        compose.onNodeWithText("Perfil screen").assertIsDisplayed()
        compose.onNode(hasText("Inicio") and hasClickAction()).performClick()
        compose.onNode(hasText("Publicar"), useUnmergedTree = true).assertIsDisplayed().performClick()
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

    @Test fun aPushedScreenHidesTheNavigationBarAndThePublishAction() {
        compose.setContent { MinkaTheme { MinkaShell(tabEntries = standIns) } }
        compose.onNode(hasText("Publicar"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Abrir detalle").performClick()
        compose.onNodeWithText("Detalle screen").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Publicar").assertCountEquals(0)
        compose.onAllNodes(hasText("Publicar"), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasText("Buscar") and hasClickAction()).assertCountEquals(0)
    }
}
