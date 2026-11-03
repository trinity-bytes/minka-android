package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VerifyCodeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val entering = VerifyCodeUiState(destination = "ana.quispe@gmail.com", attemptsLeft = 5, code = "482")

    private fun setVerify(
        uiState: VerifyCodeUiState,
        onVerify: () -> Unit = {},
        onResend: () -> Unit = {},
        onContactSupport: () -> Unit = {},
    ) {
        compose.setContent {
            MinkaTheme {
                VerifyCodeContent(
                    uiState = uiState,
                    onBack = {},
                    onCodeChange = {},
                    onVerify = onVerify,
                    onResend = onResend,
                    onContactSupport = onContactSupport,
                    onMessageShown = {},
                )
            }
        }
    }

    @Test
    fun the_code_state_shows_the_destination_the_attempts_and_the_resend_countdown() {
        setVerify(entering.copy(resendSecondsLeft = 28))

        compose.onNodeWithText("Ingresa el código").assertIsDisplayed()
        compose.onNodeWithText("Enviamos un código de 6 dígitos a ana.quispe@gmail.com.").assertIsDisplayed()
        compose.onNodeWithText("Tienes 5 intentos").assertIsDisplayed()
        compose.onNodeWithText("Reenviar código en 0:28").assertIsNotEnabled()
        compose.onNodeWithText("Verificar").assertIsNotEnabled()
    }

    @Test
    fun a_wrong_code_shows_the_attempts_left_and_resend_is_available() {
        var resends = 0
        setVerify(
            entering.copy(code = "482915", attemptsLeft = 2, codeError = CodeError.Wrong(2)),
            onResend = { resends++ },
        )

        compose.onNodeWithText("Código incorrecto · te quedan 2 intentos").assertIsDisplayed()
        compose.onNodeWithText("Verificar").assertIsNotEnabled()
        compose.onNodeWithText("Reenviar código").assertIsEnabled().performClick()
        assertEquals(1, resends)
    }

    @Test
    fun a_full_code_can_be_verified_and_support_is_reachable() {
        var verifies = 0
        var support = 0
        setVerify(entering.copy(code = "482915"), onVerify = { verifies++ }, onContactSupport = { support++ })

        compose.onNodeWithText("Verificar").assertIsEnabled().performClick()
        compose.onNodeWithText("Contáctanos").performClick()

        assertEquals(1, verifies)
        assertEquals(1, support)
    }
}
