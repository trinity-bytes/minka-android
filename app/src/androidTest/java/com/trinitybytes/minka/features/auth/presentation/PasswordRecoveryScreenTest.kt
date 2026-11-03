package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.auth.domain.ContactKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordRecoveryScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val sent = RecoverySent("ana.quispe@gmail.com", ContactKind.Email, 30)

    private fun setRecovery(
        uiState: PasswordRecoveryUiState,
        onBack: () -> Unit = {},
        onOpenInbox: (ContactKind) -> Unit = {},
        onResend: () -> Unit = {},
    ) {
        compose.setContent {
            MinkaTheme {
                PasswordRecoveryContent(
                    uiState = uiState,
                    onBack = onBack,
                    onContactChange = {},
                    onContactFocusLost = {},
                    onSubmit = {},
                    onOpenInbox = onOpenInbox,
                    onResend = onResend,
                    onMessageShown = {},
                )
            }
        }
    }

    @Test
    fun enviarEnlaceIsDisabledUntilTheContactIsValid() {
        setRecovery(PasswordRecoveryUiState(contact = "ana.quispe@"))

        compose.onNodeWithText("Recupera tu contraseña").assertIsDisplayed()
        compose.onNodeWithText("Enviar enlace").assertIsNotEnabled()
    }

    @Test
    fun enviarEnlaceIsEnabledForAPhone() {
        setRecovery(PasswordRecoveryUiState(contact = "987654321"))

        compose.onNodeWithText("Enviar enlace").assertIsEnabled()
    }

    @Test
    fun theSentStateShowsTheContactAndCountsDownBeforeReenviar() {
        setRecovery(PasswordRecoveryUiState(contact = sent.destination, sent = sent, resendSecondsLeft = 45))

        compose.onNodeWithText("Revisa tu correo").assertIsDisplayed()
        compose.onNodeWithText("Enviamos un enlace a ana.quispe@gmail.com. Vence en 30 minutos.").assertIsDisplayed()
        compose.onNodeWithText("Reenviar en 0:45").assertIsNotEnabled()
    }

    @Test
    fun reenviarAndAbrirCorreoWorkOnceTheCountdownEnds() {
        var resends = 0
        var opened: ContactKind? = null
        setRecovery(
            PasswordRecoveryUiState(contact = sent.destination, sent = sent),
            onOpenInbox = { opened = it },
            onResend = { resends++ },
        )

        compose.onNodeWithText("Reenviar").assertIsEnabled().performClick()
        compose.onNodeWithText("Abrir correo").performClick()

        assertEquals(1, resends)
        assertEquals(ContactKind.Email, opened)
    }

    @Test
    fun backReturnsFromTheSentState() {
        var back = false
        setRecovery(PasswordRecoveryUiState(contact = sent.destination, sent = sent), onBack = { back = true })

        compose.onNodeWithContentDescription("Volver").performClick()

        assertTrue(back)
    }
}
