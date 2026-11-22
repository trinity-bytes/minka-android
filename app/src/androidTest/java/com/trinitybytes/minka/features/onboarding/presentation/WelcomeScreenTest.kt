package com.trinitybytes.minka.features.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setWelcome(
        initialStep: Int = 0,
        configuration: DeviceConfigurationOverride? = null,
        onCreateAccount: () -> Unit = {},
        onSignIn: () -> Unit = {},
        onContinueWithGoogle: () -> Unit = {},
    ) {
        compose.setContent {
            var state by remember { mutableStateOf(WelcomeUiState(initialStep)) }
            val screen = @Composable {
                WelcomeContent(
                    uiState = state,
                    onNext = { state = state.copy(step = (state.step + 1).coerceAtMost(WelcomeUiState.LAST_STEP)) },
                    onSkip = { state = state.copy(step = WelcomeUiState.LAST_STEP) },
                    onStepShown = { state = state.copy(step = it) },
                    onCreateAccount = onCreateAccount,
                    onSignIn = onSignIn,
                    onContinueWithGoogle = onContinueWithGoogle,
                )
            }
            MinkaTheme {
                if (configuration == null) screen() else DeviceConfigurationOverride(configuration) { screen() }
            }
        }
    }

    private fun leftOf(text: String) = compose.onNodeWithText(text).getBoundsInRoot().left
    private fun rootWidth() = compose.onRoot().getBoundsInRoot().width

    @Test
    fun nextAdvancesToTheFollowingStep() {
        setWelcome()
        compose.onNodeWithText("Truequea lo que ya no usas, sin dinero").assertIsDisplayed()

        compose.onNodeWithText("Siguiente").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Encuentra trueques cerca de ti").assertIsDisplayed()
    }

    @Test
    fun lastStepShowsTheAccessGroupInsteadOfNext() {
        setWelcome(initialStep = WelcomeUiState.LAST_STEP)

        compose.onNodeWithText("Cuida el planeta con cada trueque").assertIsDisplayed()
        compose.onNodeWithText("Crear cuenta").assertIsDisplayed()
        compose.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        compose.onNodeWithText("Continuar con Google").assertIsDisplayed()
        compose.onAllNodesWithText("Siguiente").assertCountEquals(0)
        // Design A3: skipping leads nowhere new on the access step.
        compose.onAllNodesWithText("Omitir").assertCountEquals(0)
    }

    @Test
    fun expandedWindowShowsThePhotoAndTheTextsInTwoPanes() {
        setWelcome(
            initialStep = WelcomeUiState.LAST_STEP,
            configuration = DeviceConfigurationOverride.ForcedSize(DpSize(1000.dp, 700.dp)),
        )

        // The texts and actions sit in the end pane, beside the photo.
        assertTrue(leftOf("Cuida el planeta con cada trueque") > rootWidth() * 0.4f)
        compose.onNodeWithText("Continuar con Google").assertIsDisplayed()
    }

    @Test
    fun compactWindowStacksTheTextsUnderThePhoto() {
        setWelcome(
            initialStep = WelcomeUiState.LAST_STEP,
            configuration = DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp)),
        )

        assertTrue(leftOf("Cuida el planeta con cada trueque") < rootWidth() * 0.1f)
    }

    @Test
    fun accessButtonsStayReachableInPhoneLandscapeAtDoubleFontScale() {
        setWelcome(
            initialStep = WelcomeUiState.LAST_STEP,
            configuration = DeviceConfigurationOverride.FontScale(2f) then
                DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 360.dp)),
        )

        compose.onNodeWithText("Continuar con Google").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun skipGoesToTheAccessGroup() {
        setWelcome()

        compose.onNodeWithText("Omitir").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Crear cuenta").assertIsDisplayed()
    }

    @Test
    fun accessButtonsRunTheirActions() {
        val clicks = mutableListOf<String>()
        setWelcome(
            initialStep = WelcomeUiState.LAST_STEP,
            onCreateAccount = { clicks += "register" },
            onSignIn = { clicks += "signIn" },
            onContinueWithGoogle = { clicks += "google" },
        )

        compose.onNodeWithText("Crear cuenta").performClick()
        compose.onNodeWithText("Iniciar sesión").performClick()
        compose.onNodeWithText("Continuar con Google").performClick()

        assertEquals(listOf("register", "signIn", "google"), clicks)
    }
}
