package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.Register
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.PasswordStrength
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.TERMS_VERSION
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRegistrationRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = RegisterViewModel(Register(repository, TEST_CLOCK))

    private val RegisterViewModel.state get() = uiState.value

    private fun RegisterViewModel.fillValidForm() {
        onNameChange("Luis Mamani")
        onContactChange("luis.mamani@gmail.com")
        onPasswordChange("Trueque2026!")
        onConfirmPasswordChange("Trueque2026!")
        onTermsChange(true)
    }

    @Test
    fun `non-secret form input restores and persists but passwords do not`() {
        val handle = SavedStateHandle(mapOf("register_name" to "Ana", "register_contact" to "ana@example.com", "register_terms" to true))
        val model = RegisterViewModel(Register(repository, TEST_CLOCK), handle)
        assertEquals("Ana", model.state.name)
        assertEquals("ana@example.com", model.state.contact)
        assertTrue(model.state.termsAccepted)
        model.onNameChange("Luis")
        model.onContactChange("luis@example.com")
        model.onTermsChange(false)
        model.onPasswordChange("secret")
        model.onConfirmPasswordChange("secret")
        assertEquals("Luis", handle.get<String>("register_name"))
        assertEquals("luis@example.com", handle.get<String>("register_contact"))
        assertEquals(false, handle.get<Boolean>("register_terms"))
        assertFalse(handle.keys().any { it.contains("password", ignoreCase = true) })
    }

    @Test
    fun `continuar waits until every field is valid`() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.canSubmit)

        viewModel.onNameChange("Luis Mamani")
        viewModel.onContactChange("luis.mamani@gmail.com")
        viewModel.onPasswordChange("Trueque2026!")
        assertFalse(viewModel.state.canSubmit)
        viewModel.onConfirmPasswordChange("Trueque2026")
        viewModel.onTermsChange(true)
        assertFalse("passwords differ", viewModel.state.canSubmit)

        viewModel.onConfirmPasswordChange("Trueque2026!")
        assertTrue(viewModel.state.canSubmit)

        viewModel.onTermsChange(false)
        assertFalse("terms not accepted", viewModel.state.canSubmit)
        viewModel.onTermsChange(true)
        viewModel.onContactChange("luis.mamani@")
        assertFalse("invalid contact", viewModel.state.canSubmit)
        viewModel.onContactChange("987 111 222")
        assertTrue("a phone is accepted too", viewModel.state.canSubmit)
        viewModel.onPasswordChange("abcdefgh")
        viewModel.onConfirmPasswordChange("abcdefgh")
        assertFalse("weak password", viewModel.state.canSubmit)
        viewModel.onPasswordChange("Trueque2026!")
        viewModel.onConfirmPasswordChange("Trueque2026!")
        viewModel.onNameChange("  ")
        assertFalse("blank name", viewModel.state.canSubmit)
    }

    @Test
    fun `submitting an incomplete form sends nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onNameChange("Luis Mamani")

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(repository.registrations.isEmpty())
    }

    @Test
    fun `the strength meter follows the password as it is typed`() {
        val viewModel = viewModel()
        assertNull(viewModel.state.strength)

        viewModel.onPasswordChange("abc")
        assertEquals(PasswordStrength.Weak, viewModel.state.strength)
        viewModel.onPasswordChange("trueque2026")
        assertEquals(PasswordStrength.Medium, viewModel.state.strength)
        viewModel.onPasswordChange("Trueque2026!")
        assertEquals(PasswordStrength.Strong, viewModel.state.strength)
        viewModel.onPasswordChange("")
        assertNull(viewModel.state.strength)
    }

    @Test
    fun `each field shows its error when it loses focus and the message follows the typing`() {
        val viewModel = viewModel()
        viewModel.onContactFocusLost()
        viewModel.onPasswordFocusLost()
        viewModel.onConfirmPasswordFocusLost()
        assertNull("untouched fields are not flagged", viewModel.state.contactError)
        assertNull(viewModel.state.passwordError)
        assertNull(viewModel.state.confirmError)

        viewModel.onContactChange("luis.mamani@")
        assertNull("no error while typing", viewModel.state.contactError)
        viewModel.onContactFocusLost()
        assertEquals(RegisterFieldError.InvalidContact, viewModel.state.contactError)
        viewModel.onContactChange("luis.mamani@gmail.com")
        assertNull(viewModel.state.contactError)

        viewModel.onPasswordChange("Tru1")
        viewModel.onPasswordFocusLost()
        assertEquals(RegisterFieldError.ShortPassword, viewModel.state.passwordError)
        viewModel.onPasswordChange("abcdefgh")
        assertEquals(RegisterFieldError.WeakPassword, viewModel.state.passwordError)
        viewModel.onPasswordChange("Trueque2026!")
        assertNull(viewModel.state.passwordError)

        viewModel.onConfirmPasswordChange("Trueque2026")
        viewModel.onConfirmPasswordFocusLost()
        assertEquals(RegisterFieldError.PasswordMismatch, viewModel.state.confirmError)
        viewModel.onConfirmPasswordChange("Trueque2026!")
        assertNull(viewModel.state.confirmError)
        viewModel.onPasswordChange("Trueque2027!")
        assertEquals("changing the password rechecks the confirmation", RegisterFieldError.PasswordMismatch, viewModel.state.confirmError)
    }

    @Test
    fun `debes aceptar los terminos shows when the terms are left unchecked`() {
        val viewModel = viewModel()
        viewModel.onNameChange("Luis Mamani")
        viewModel.onContactChange("luis.mamani@gmail.com")
        viewModel.onPasswordChange("Trueque2026!")
        viewModel.onConfirmPasswordChange("Trueque2026!")
        assertFalse(viewModel.state.termsError)

        // Leaving the last field with the terms unchecked.
        viewModel.onConfirmPasswordFocusLost()
        assertTrue(viewModel.state.termsError)
        viewModel.onTermsChange(true)
        assertFalse(viewModel.state.termsError)

        // Unchecking them again.
        viewModel.onTermsChange(false)
        assertTrue(viewModel.state.termsError)
    }

    @Test
    fun `submitting sends the terms version and the date, then opens the code step`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AuthOutcome<VerificationChallenge>>()
        repository.registerAnswer = { pending.await() }
        val viewModel = viewModel()
        viewModel.fillValidForm()
        viewModel.onNameChange(" Luis Mamani ")
        viewModel.onContactChange(" luis.mamani@gmail.com ")

        viewModel.submit()
        runCurrent()
        assertTrue(viewModel.state.isLoading)
        assertFalse(viewModel.state.fieldsEnabled)
        assertFalse(viewModel.state.canSubmit)
        viewModel.onNameChange("changed while loading")
        assertEquals(" Luis Mamani ", viewModel.state.name)

        pending.complete(Outcome.Success(TEST_CHALLENGE))
        runCurrent()

        assertEquals(
            listOf(Registration("Luis Mamani", "luis.mamani@gmail.com", "Trueque2026!", TERMS_VERSION, TEST_NOW)),
            repository.registrations,
        )
        assertEquals("1.0", repository.registrations.single().termsVersion)
        assertFalse(viewModel.state.isLoading)
        assertEquals(RegisterEvent.CodeSent(TEST_CHALLENGE), viewModel.events.first())
    }

    @Test
    fun `a registered email shows este correo ya esta registrado until the contact changes`() = runTest(dispatcher) {
        repository.registerAnswer = { Outcome.Failure(AuthError.ContactTaken) }
        val viewModel = viewModel()
        viewModel.fillValidForm()
        viewModel.onContactChange("ana.quispe@gmail.com")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RegisterFieldError.ContactTaken, viewModel.state.contactError)
        assertFalse("the button waits for a different contact", viewModel.state.canSubmit)
        assertNull(viewModel.state.message)

        viewModel.onContactChange("ana.quispe2@gmail.com")
        assertNull(viewModel.state.contactError)
        assertTrue(viewModel.state.canSubmit)
    }

    @Test
    fun `offline keeps what was typed and offers reintentar in a snackbar`() = runTest(dispatcher) {
        repository.registerAnswer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.fillValidForm()

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("1.22", RegisterMessage.Offline, viewModel.state.message)
        assertEquals("Luis Mamani", viewModel.state.name)
        assertEquals("luis.mamani@gmail.com", viewModel.state.contact)
        assertEquals("Trueque2026!", viewModel.state.password)
        assertEquals("Trueque2026!", viewModel.state.confirmPassword)
        assertTrue(viewModel.state.termsAccepted)
        assertFalse(viewModel.state.isLoading)
        assertTrue(viewModel.state.canSubmit)

        repository.registerAnswer = { Outcome.Failure(AuthError.Unexpected) }
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(RegisterMessage.Unexpected, viewModel.state.message)
    }

    @Test
    fun `reintentar registers again with what the form holds now`() = runTest(dispatcher) {
        repository.registerAnswer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.fillValidForm()
        viewModel.submit()
        advanceUntilIdle()

        viewModel.onNameChange("Luis A. Mamani")
        repository.registerAnswer = { Outcome.Success(TEST_CHALLENGE) }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf("Luis Mamani", "Luis A. Mamani"), repository.registrations.map { it.name })
        assertNull(viewModel.state.message)
        assertEquals(RegisterEvent.CodeSent(TEST_CHALLENGE), viewModel.events.first())
    }
}
