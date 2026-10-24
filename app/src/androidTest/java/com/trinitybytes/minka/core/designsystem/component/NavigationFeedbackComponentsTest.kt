package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFeedbackComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun topAppBarShowsTheTitleAndGoesBack() {
        var backs = 0
        compose.setContent { MinkaTheme { MinkaTopAppBar(title = "Detalle", onBack = { backs++ }) } }

        compose.onNodeWithText("Detalle").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun largeTopAppBarShowsTheTitle() {
        compose.setContent { MinkaTheme { MinkaTopAppBar(title = "Explorar", size = MinkaTopAppBarSize.Large) } }

        compose.onNodeWithText("Explorar").assertIsDisplayed()
    }

    @Test
    fun logoIsAnnouncedByTheAppName() {
        compose.setContent { MinkaTheme { MinkaLogo() } }

        compose.onNodeWithContentDescription("Mink’a").assertIsDisplayed()
    }

    @Test
    fun badgeShowsItsLabel() {
        compose.setContent { MinkaTheme { MinkaBadge(text = "Como nuevo", tone = MinkaBadgeTone.Lavender) } }

        compose.onNodeWithText("Como nuevo").assertIsDisplayed()
    }

    @Test
    fun snackbarActionReportsActionPerformed() {
        val hostState = SnackbarHostState()
        var result: SnackbarResult? = null
        compose.setContent {
            MinkaTheme {
                MinkaSnackbarHost(hostState)
                LaunchedEffect(Unit) {
                    result = hostState.showSnackbar(message = "Publicación guardada", actionLabel = "Deshacer")
                }
            }
        }

        compose.onNodeWithText("Publicación guardada").assertIsDisplayed()
        compose.onNodeWithText("Deshacer").performClick()
        compose.waitForIdle()

        assertEquals(SnackbarResult.ActionPerformed, result)
    }

    @Test
    fun dialogConfirmsAndDismisses() {
        var confirmed = 0
        var dismissed = 0
        compose.setContent {
            MinkaTheme {
                MinkaDialog(
                    title = "¿Confirmar el trueque?",
                    text = "El objeto quedará reservado hasta que ambos confirmen la entrega.",
                    confirmText = "Confirmar",
                    onConfirm = { confirmed++ },
                    dismissText = "Cancelar",
                    onDismiss = { dismissed++ },
                    icon = Icons.Rounded.SwapHoriz,
                )
            }
        }

        compose.onNodeWithText("¿Confirmar el trueque?").assertIsDisplayed()
        compose.onNodeWithText("Confirmar").performClick()
        compose.onNodeWithText("Cancelar").performClick()

        assertEquals(1, confirmed)
        assertEquals(1, dismissed)
    }
}
