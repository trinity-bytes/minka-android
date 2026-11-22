package com.trinitybytes.minka.navigation

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.onboarding.presentation.SplashDestination
import com.trinitybytes.minka.features.onboarding.presentation.SplashUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MinkaShellTest {
    @get:Rule val compose = createComposeRule()
    @Test fun destinationsSwitchAndPublishActionFires() {
        var published = false
        compose.setContent { MinkaTheme { MinkaShell { published = true } } }
        compose.onNodeWithText("Inicio").assertIsSelected()
        compose.onNode(hasText("Buscar") and hasClickAction()).performClick().assertIsSelected()
        compose.onNode(hasText("Perfil") and hasClickAction()).performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Publicar").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(published) }
    }
    @Test fun launchAndSelectedTabSurviveRestorationAndPublishCloses() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MinkaTheme { MinkaNavHost(SplashUiState.Ready(SplashDestination.Home)) } }
        compose.onNode(hasText("Buscar") and hasClickAction()).performClick().assertIsSelected()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasText("Buscar") and hasClickAction()).assertIsSelected()
        compose.onNodeWithContentDescription("Publicar").performClick()
        compose.onNodeWithContentDescription("Cerrar").assertIsDisplayed().performClick()
        compose.onNode(hasText("Buscar") and hasClickAction()).assertIsSelected()
    }
}
