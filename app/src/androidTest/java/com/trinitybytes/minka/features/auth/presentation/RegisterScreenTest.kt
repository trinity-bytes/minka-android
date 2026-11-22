package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val validForm = RegisterUiState(
        name = "Ana Quispe",
        contact = "ana.quispe@gmail.com",
        password = "Minka2026!",
        confirmPassword = "Minka2026!",
        termsAccepted = true,
    )

    private fun setRegister(
        uiState: RegisterUiState,
        onTermsChange: (Boolean) -> Unit = {},
        onSubmit: () -> Unit = {},
    ) {
        compose.setContent {
            MinkaTheme {
                RegisterContent(
                    uiState = uiState,
                    onBack = {},
                    onNameChange = {},
                    onContactChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onTermsChange = onTermsChange,
                    onContactFocusLost = {},
                    onPasswordFocusLost = {},
                    onConfirmPasswordFocusLost = {},
                    onSubmit = onSubmit,
                    onMessageShown = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun the_data_state_shows_the_step_the_meter_and_the_terms_without_a_version() {
        var submits = 0
        setRegister(validForm, onSubmit = { submits++ })

        compose.onNodeWithText("Paso 1 de 3 · Cuenta").assertIsDisplayed()
        compose.onNodeWithText("Crea tu cuenta").assertIsDisplayed()
        compose.onNodeWithText("Fuerte").assertIsDisplayed()
        compose.onNodeWithText("Acepto los términos y la privacidad").assertIsDisplayed()
        compose.onNodeWithText("Crea tu cuenta").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Continuar").assertIsEnabled().performClick()
        assertEquals(1, submits)
    }

    @Test
    fun the_error_state_shows_each_message_and_disables_continuar() {
        setRegister(
            validForm.copy(
                confirmPassword = "Minka2025!",
                termsAccepted = false,
                contactError = RegisterFieldError.ContactTaken,
                confirmError = RegisterFieldError.PasswordMismatch,
                termsError = true,
            ),
        )

        compose.onNodeWithText("Este correo ya está registrado").assertIsDisplayed()
        compose.onNodeWithText("Las contraseñas no coinciden").assertIsDisplayed()
        compose.onNodeWithText("Debes aceptar los términos").assertIsDisplayed()
        compose.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Test
    fun tapping_the_terms_toggles_them() {
        var accepted: Boolean? = null
        setRegister(validForm.copy(termsAccepted = false), onTermsChange = { accepted = it })

        compose.onNodeWithText("Acepto los términos y la privacidad").performClick()

        assertEquals(true, accepted)
    }

    @Test
    fun both_password_fields_have_a_visibility_toggle() {
        setRegister(validForm)

        compose.onAllNodesWithContentDescription("Mostrar contraseña").assertCountEquals(2)
    }

    @Test
    fun offline_keeps_the_form_and_the_snackbar_retries() {
        var retries = 0
        compose.setContent {
            MinkaTheme {
                RegisterContent(
                    uiState = validForm.copy(message = RegisterMessage.Offline),
                    onBack = {},
                    onNameChange = {},
                    onContactChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onTermsChange = {},
                    onContactFocusLost = {},
                    onPasswordFocusLost = {},
                    onConfirmPasswordFocusLost = {},
                    onSubmit = {},
                    onMessageShown = {},
                    onRetry = { retries++ },
                )
            }
        }

        compose.onNodeWithText("Sin conexión. Lo escrito sigue aquí.").assertIsDisplayed()
        compose.onNodeWithText("Ana Quispe", substring = true).assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        compose.waitForIdle()

        assertEquals(1, retries)
    }
}
