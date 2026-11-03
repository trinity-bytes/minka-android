package com.trinitybytes.minka.features.auth.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.RequestPasswordReset
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.ContactKind
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class PasswordRecoveryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PasswordRecoveryViewModel(RequestPasswordReset(repository))

    private val PasswordRecoveryViewModel.state get() = uiState.value

    @Test
    fun `enviar enlace stays disabled while the field is invalid`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertFalse(viewModel.state.canSubmit)

        viewModel.onContactChange("ana.quispe@")
        assertFalse(viewModel.state.canSubmit)
        viewModel.onContactFocusLost()
        assertTrue(viewModel.state.contactError)

        viewModel.submit()
        advanceUntilIdle()
        assertTrue(repository.resets.isEmpty())
        assertNull(viewModel.state.sent)

        viewModel.onContactChange("ana.quispe@gmail.com")
        assertTrue(viewModel.state.canSubmit)
        assertFalse(viewModel.state.contactError)
    }

    @Test
    fun `an email and a phone are both accepted, as in registration`() {
        val viewModel = viewModel()

        viewModel.onContactChange("ana.quispe@gmail.com")
        assertTrue(viewModel.state.canSubmit)

        viewModel.onContactChange("+51 987 654 321")
        assertTrue(viewModel.state.canSubmit)
        viewModel.onContactFocusLost()
        assertFalse(viewModel.state.contactError)

        viewModel.onContactChange("12345")
        assertFalse(viewModel.state.canSubmit)
    }

    @Test
    fun `submitting loads, then moves to the sent state with the contact`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AuthOutcome<PasswordReset>>()
        repository.answer = { pending.await() }
        val viewModel = viewModel()
        viewModel.onContactChange(" ana.quispe@gmail.com ")

        viewModel.submit()
        runCurrent()
        assertTrue(viewModel.state.isLoading)
        assertFalse(viewModel.state.fieldsEnabled)
        assertFalse(viewModel.state.canSubmit)

        pending.complete(Outcome.Success(PasswordReset("ana.quispe@gmail.com", 30, 60)))
        runCurrent()

        assertFalse(viewModel.state.isLoading)
        assertEquals(RecoverySent("ana.quispe@gmail.com", ContactKind.Email, 30), viewModel.state.sent)
        assertEquals(listOf("ana.quispe@gmail.com"), repository.resets)
    }

    @Test
    fun `a phone is sent as a phone`() = runTest(dispatcher) {
        repository.answer = { Outcome.Success(PasswordReset("987654321", 30, 60)) }
        val viewModel = viewModel()
        viewModel.onContactChange("+51 987 654 321")

        viewModel.submit()
        runCurrent()

        assertEquals(RecoverySent("987654321", ContactKind.Phone, 30), viewModel.state.sent)
    }

    @Test
    fun `reenviar stays disabled until the countdown ends, then a resend restarts it`() = runTest(dispatcher) {
        repository.answer = { Outcome.Success(PasswordReset("ana.quispe@gmail.com", 30, 60)) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.submit()
        runCurrent()

        assertEquals(60L, viewModel.state.resendSecondsLeft)
        assertFalse(viewModel.state.canResend)
        viewModel.resend()
        runCurrent()
        assertEquals(1, repository.resets.size)

        advanceTimeBy(15_001)
        assertEquals(45L, viewModel.state.resendSecondsLeft)
        assertEquals("0:45", formatCountdown(viewModel.state.resendSecondsLeft!!))
        assertFalse(viewModel.state.canResend)

        advanceTimeBy(45_000)
        assertNull(viewModel.state.resendSecondsLeft)
        assertTrue(viewModel.state.canResend)

        viewModel.resend()
        runCurrent()
        assertEquals(listOf("ana.quispe@gmail.com", "ana.quispe@gmail.com"), repository.resets)
        assertEquals(60L, viewModel.state.resendSecondsLeft)
        assertFalse(viewModel.state.canResend)
        assertEquals(RecoverySent("ana.quispe@gmail.com", ContactKind.Email, 30), viewModel.state.sent)
    }

    @Test
    fun `an offline answer is reported and keeps what was typed`() = runTest(dispatcher) {
        repository.answer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")

        viewModel.submit()
        advanceUntilIdle()

        assertTrue("1.22", viewModel.state.isOffline)
        assertNull(viewModel.state.message)
        assertEquals("ana.quispe@gmail.com", viewModel.state.contact)
        assertNull(viewModel.state.sent)
        assertFalse(viewModel.state.isLoading)
        assertTrue(viewModel.state.canSubmit)
    }

    @Test
    fun `reintentar sends the link again to what was typed and leaves 1_22 once it works`() = runTest(dispatcher) {
        repository.answer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.onContactChange("987654321")
        viewModel.submit()
        advanceUntilIdle()

        repository.answer = { Outcome.Success(PasswordReset("987654321", 30, 60)) }
        viewModel.retry()
        runCurrent()

        assertEquals(listOf("987654321", "987654321"), repository.resets)
        assertFalse(viewModel.state.isOffline)
        assertEquals(RecoverySent("987654321", ContactKind.Phone, 30), viewModel.state.sent)
    }

    @Test
    fun `another error is reported and keeps what was typed`() = runTest(dispatcher) {
        repository.answer = { Outcome.Failure(AuthError.Unexpected) }
        val viewModel = viewModel()
        viewModel.onContactChange("987654321")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(RecoveryMessage.Unexpected, viewModel.state.message)
        assertEquals("987654321", viewModel.state.contact)
        assertTrue(viewModel.state.canSubmit)
    }

    @Test
    fun `a failed resend keeps the sent state and lets the user try again`() = runTest(dispatcher) {
        repository.answer = { Outcome.Success(PasswordReset("ana.quispe@gmail.com", 30, 60)) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.submit()
        advanceTimeBy(60_001)
        assertTrue(viewModel.state.canResend)

        repository.answer = { Outcome.Failure(AuthError.Offline) }
        viewModel.resend()
        runCurrent()

        assertTrue("1.22", viewModel.state.isOffline)
        assertEquals(RecoverySent("ana.quispe@gmail.com", ContactKind.Email, 30), viewModel.state.sent)
        assertTrue(viewModel.state.canResend)
    }

    private class FakeRepository : AuthRepository {
        var answer: suspend () -> AuthOutcome<PasswordReset> = { Outcome.Success(PasswordReset("x", 30, 60)) }
        val resets = mutableListOf<String>()

        override suspend fun requestPasswordReset(contact: String): AuthOutcome<PasswordReset> {
            resets += contact
            return answer()
        }

        override suspend fun signIn(contact: String, password: String): AuthOutcome<Session> = TODO()
        override suspend fun googleAccounts(): AuthOutcome<List<GoogleAccount>> = TODO()
        override suspend fun signInWithGoogle(accountId: String): AuthOutcome<Session> = TODO()
        override suspend fun register(registration: Registration): AuthOutcome<VerificationChallenge> = TODO()
        override suspend fun verifyCode(challengeId: String, code: String): AuthOutcome<Session> = TODO()
        override suspend fun resendCode(challengeId: String): AuthOutcome<VerificationChallenge> = TODO()
    }
}
