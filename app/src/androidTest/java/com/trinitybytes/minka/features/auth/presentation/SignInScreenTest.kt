package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setSignIn(
        uiState: SignInUiState,
        onForgotPassword: () -> Unit = {},
        onDismissLock: () -> Unit = {},
        onChooseGoogleAccount: (String) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            MinkaTheme {
                SignInContent(
                    uiState = uiState,
                    onBack = {},
                    onContactChange = {},
                    onPasswordChange = {},
                    onContactFocusLost = {},
                    onPasswordFocusLost = {},
                    onSubmit = {},
                    onForgotPassword = onForgotPassword,
                    onGoogle = {},
                    onRegister = {},
                    onChooseGoogleAccount = onChooseGoogleAccount,
                    onDismissGoogle = {},
                    onDismissLock = onDismissLock,
                    onMessageShown = {},
                    onRetry = onRetry,
                )
            }
        }
    }

    @Test
    fun offlineShowsSinConexionAndReintentarRetries() {
        var retries = 0
        setSignIn(SignInUiState(contact = "ana.quispe@gmail.com", password = "Minka2026!", isOffline = true), onRetry = { retries++ })

        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Revisa tu wifi o tus datos móviles. Lo que escribiste se guardó.").assertIsDisplayed()
        compose.onNodeWithText("Ingresar").assertDoesNotExist()
        compose.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun ingresarIsDisabledWhileAFieldIsEmpty() {
        setSignIn(SignInUiState(contact = "ana.quispe@gmail.com"))

        compose.onNodeWithText("Ingresar").assertIsNotEnabled()
    }

    @Test
    fun fieldErrorsShowBelowEachField() {
        setSignIn(
            SignInUiState(
                contact = "ana.quispe@",
                password = "1234",
                contactError = SignInFieldError.InvalidContact,
                passwordError = SignInFieldError.ShortPassword,
            ),
        )

        compose.onNodeWithText("Ingresa un correo o teléfono válido").assertIsDisplayed()
        compose.onNodeWithText("Mínimo 8 caracteres").assertIsDisplayed()
        compose.onNodeWithText("Ingresar").assertIsEnabled()
    }

    @Test
    fun loadingLocksTheFieldsAndShowsTheIndicator() {
        setSignIn(SignInUiState(contact = "ana.quispe@gmail.com", password = "Minka2026!", isLoading = true))

        compose.onAllNodes(hasSetTextAction()).assertCountEquals(2).assertAll(isNotEnabled())
        compose.onNodeWithContentDescription("Ingresar").assertIsDisplayed()
        compose.onNodeWithText("Continuar con Google").assertIsNotEnabled()
    }

    @Test
    fun theLockDialogCountsDownAndLeadsToRecovery() {
        var recovered = false
        setSignIn(SignInUiState(lockSecondsLeft = 899), onForgotPassword = { recovered = true })

        compose.onNodeWithText("Cuenta bloqueada").assertIsDisplayed()
        compose.onNode(hasText("14:59", substring = true)).assertIsDisplayed()
        compose.onNodeWithText("Recuperar").performClick()

        assertTrue(recovered)
    }

    @Test
    fun choosingAGoogleAccountReportsIt() {
        var chosen: String? = null
        setSignIn(
            SignInUiState(
                googleAccounts = listOf(
                    GoogleAccount("google-ana", "Ana Quispe", "ana.quispe@gmail.com"),
                    GoogleAccount("google-ana-work", "Ana Q. · Trabajo", "ana@reflowtech.pe"),
                ),
            ),
            onChooseGoogleAccount = { chosen = it },
        )

        compose.onNodeWithText("Elige una cuenta").assertIsDisplayed()
        compose.onNodeWithText("Ana Q. · Trabajo").performClick()

        assertEquals("google-ana-work", chosen)
    }
}
