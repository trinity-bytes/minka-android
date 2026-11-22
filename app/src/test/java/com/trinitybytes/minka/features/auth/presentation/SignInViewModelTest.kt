package com.trinitybytes.minka.features.auth.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.GetGoogleAccounts
import com.trinitybytes.minka.features.auth.application.SignIn
import com.trinitybytes.minka.features.auth.application.SignInWithGoogle
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import com.trinitybytes.minka.features.onboarding.FakeProgressStore
import com.trinitybytes.minka.features.onboarding.application.ResolveLaunch
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class SignInViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val store = FakeSessionStore()
    private val progressStore = FakeProgressStore()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SignInViewModel(
        SignIn(repository, store),
        GetGoogleAccounts(repository),
        SignInWithGoogle(repository, store),
        ResolveLaunch(store, progressStore),
    )

    private val SignInViewModel.state get() = uiState.value

    @Test
    fun `ingresar stays disabled until both fields are filled`() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.canSubmit)

        viewModel.onContactChange("ana.quispe@gmail.com")
        assertFalse(viewModel.state.canSubmit)

        viewModel.onPasswordChange("Minka2026!")
        assertTrue(viewModel.state.canSubmit)

        viewModel.onContactChange("  ")
        assertFalse(viewModel.state.canSubmit)
    }

    @Test
    fun `each field is validated when it loses focus and shows its message below it`() {
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@")
        viewModel.onPasswordChange("1234")
        assertNull(viewModel.state.contactError)
        assertNull(viewModel.state.passwordError)

        viewModel.onContactFocusLost()
        assertEquals(SignInFieldError.InvalidContact, viewModel.state.contactError)
        assertNull(viewModel.state.passwordError)

        viewModel.onPasswordFocusLost()
        assertEquals(SignInFieldError.ShortPassword, viewModel.state.passwordError)

        viewModel.onContactChange("ana.quispe@gmail.com")
        assertNull(viewModel.state.contactError)
        viewModel.onPasswordChange("Minka2026!")
        assertNull(viewModel.state.passwordError)
    }

    @Test
    fun `an empty field that loses focus is not flagged yet`() {
        val viewModel = viewModel()

        viewModel.onContactFocusLost()
        viewModel.onPasswordFocusLost()

        assertNull(viewModel.state.contactError)
        assertNull(viewModel.state.passwordError)
    }

    @Test
    fun `submitting invalid fields shows their messages without calling the server`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onContactChange("ana")
        viewModel.onPasswordChange("123")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SignInFieldError.InvalidContact, viewModel.state.contactError)
        assertEquals(SignInFieldError.ShortPassword, viewModel.state.passwordError)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun `while signing in the button loads and the fields are locked, then the session is kept`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AuthOutcome<Session>>()
        repository.signInAnswer = { pending.await() }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.onPasswordChange("Minka2026!")

        viewModel.submit()
        runCurrent()

        assertTrue(viewModel.state.isLoading)
        assertFalse(viewModel.state.fieldsEnabled)
        assertFalse(viewModel.state.canSubmit)
        viewModel.onContactChange("other@gmail.com")
        assertEquals("ana.quispe@gmail.com", viewModel.state.contact)

        pending.complete(Outcome.Success(session))
        advanceUntilIdle()

        assertEquals(SignInEvent.SignedIn(pendingStep = null), viewModel.events.first())
        assertEquals(session, store.session.value)
        assertEquals(listOf("ana.quispe@gmail.com" to "Minka2026!"), repository.signIns)
    }

    @Test
    fun `wrong credentials unlock the form and report the attempts left`() = runTest(dispatcher) {
        repository.signInAnswer = { Outcome.Failure(AuthError.InvalidCredentials(attemptsLeft = 3)) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.onPasswordChange("Equivocada1")

        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.state.isLoading)
        assertEquals(SignInMessage.WrongCredentials(attemptsLeft = 3), viewModel.state.message)
        assertNull(store.session.value)

        viewModel.onMessageShown()
        assertNull(viewModel.state.message)
    }

    @Test
    fun `a locked account shows the dialog with a countdown that ticks down to zero`() = runTest(dispatcher) {
        repository.signInAnswer = { Outcome.Failure(AuthError.AccountLocked(retryAfterSeconds = 900)) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.onPasswordChange("Equivocada5")

        viewModel.submit()
        runCurrent()
        assertEquals(900L, viewModel.state.lockSecondsLeft)
        assertEquals("15:00", formatCountdown(viewModel.state.lockSecondsLeft!!))
        assertFalse(viewModel.state.isLoading)

        advanceTimeBy(1_001)
        assertEquals(899L, viewModel.state.lockSecondsLeft)
        assertEquals("14:59", formatCountdown(viewModel.state.lockSecondsLeft!!))

        advanceTimeBy(899_000)
        assertNull(viewModel.state.lockSecondsLeft)
    }

    @Test
    fun `the lock dialog leads to password recovery or closes`() = runTest(dispatcher) {
        repository.signInAnswer = { Outcome.Failure(AuthError.AccountLocked(retryAfterSeconds = 60)) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.onPasswordChange("Equivocada5")

        viewModel.submit()
        runCurrent()
        viewModel.recoverPassword()
        runCurrent()

        assertNull(viewModel.state.lockSecondsLeft)
        assertEquals(SignInEvent.OpenRecovery, viewModel.events.first())

        viewModel.submit()
        runCurrent()
        viewModel.dismissLock()
        assertNull(viewModel.state.lockSecondsLeft)
    }

    @Test
    fun `opening with google shows the account sheet once`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onStart(withGoogle = true)
        advanceUntilIdle()
        assertEquals(accounts, viewModel.state.googleAccounts)

        viewModel.dismissGoogleSheet()
        viewModel.onStart(withGoogle = true)
        advanceUntilIdle()
        assertNull(viewModel.state.googleAccounts)
        assertEquals(1, repository.googleAccountCalls)
    }

    @Test
    fun `without google the sheet stays closed`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onStart(withGoogle = false)
        advanceUntilIdle()

        assertNull(viewModel.state.googleAccounts)
        assertEquals(0, repository.googleAccountCalls)
    }

    @Test
    fun `choosing a google account closes the sheet, loads and signs in`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AuthOutcome<Session>>()
        repository.googleAnswer = { pending.await() }
        val viewModel = viewModel()
        viewModel.openGoogleSheet()
        advanceUntilIdle()

        viewModel.chooseGoogleAccount(accounts[1].id)
        runCurrent()

        assertNull(viewModel.state.googleAccounts)
        assertTrue(viewModel.state.isLoading)
        assertFalse(viewModel.state.fieldsEnabled)

        pending.complete(Outcome.Success(session))
        advanceUntilIdle()

        assertEquals(SignInEvent.SignedIn(pendingStep = null), viewModel.events.first())
        assertEquals(listOf(accounts[1].id), repository.googleSignIns)
        assertEquals(session, store.session.value)
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun `an offline answer is reported and keeps what was typed`() = runTest(dispatcher) {
        repository.signInAnswer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.onPasswordChange("Minka2026!")

        viewModel.submit()
        advanceUntilIdle()

        assertTrue("1.22", viewModel.state.isOffline)
        assertNull(viewModel.state.message)
        assertEquals("ana.quispe@gmail.com", viewModel.state.contact)
        assertEquals("Minka2026!", viewModel.state.password)
        assertTrue(viewModel.state.canSubmit)
    }

    @Test
    fun `reintentar signs in again with what was typed and leaves 1_22 once it works`() = runTest(dispatcher) {
        repository.signInAnswer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.onPasswordChange("Minka2026!")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()
        assertTrue("still offline", viewModel.state.isOffline)
        assertEquals(2, repository.signInCalls)

        repository.signInAnswer = { Outcome.Success(session) }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(List(3) { "ana.quispe@gmail.com" to "Minka2026!" }, repository.signIns)
        assertFalse(viewModel.state.isOffline)
        assertEquals(SignInEvent.SignedIn(pendingStep = null), viewModel.events.first())
    }

    @Test
    fun `offline while opening google shows 1_22 and reintentar opens the sheet`() = runTest(dispatcher) {
        repository.accountsAnswer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.openGoogleSheet()
        advanceUntilIdle()
        assertTrue(viewModel.state.isOffline)
        assertNull(viewModel.state.googleAccounts)

        repository.accountsAnswer = { Outcome.Success(accounts) }
        viewModel.retry()
        advanceUntilIdle()

        assertFalse(viewModel.state.isOffline)
        assertEquals(accounts, viewModel.state.googleAccounts)
    }

    @Test
    fun `signing in resumes the profile setup this account left pending`() = runTest(dispatcher) {
        progressStore.progress.value = OnboardingProgress.Pending(session.user.id, SetupStep.Interests)
        val viewModel = viewModel()
        viewModel.onContactChange("ana.quispe@gmail.com")
        viewModel.onPasswordChange("Minka2026!")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SignInEvent.SignedIn(pendingStep = SetupStep.Interests), viewModel.events.first())
        assertEquals(session, store.session.value)
    }

    @Test
    fun `signing in with google resumes the profile setup this account left pending`() = runTest(dispatcher) {
        progressStore.progress.value = OnboardingProgress.Pending(session.user.id, SetupStep.Notifications)
        val viewModel = viewModel()
        viewModel.openGoogleSheet()
        advanceUntilIdle()

        viewModel.chooseGoogleAccount(accounts[0].id)
        advanceUntilIdle()

        assertEquals(SignInEvent.SignedIn(pendingStep = SetupStep.Notifications), viewModel.events.first())
    }

    @Test
    fun `a setup left pending by another account or already completed opens inicio`() = runTest(dispatcher) {
        for (progress in listOf(
            OnboardingProgress.Pending("user-luis", SetupStep.Interests),
            OnboardingProgress.Completed(session.user.id),
        )) {
            progressStore.progress.value = progress
            store.session.value = null
            val viewModel = viewModel()
            viewModel.onContactChange("ana.quispe@gmail.com")
            viewModel.onPasswordChange("Minka2026!")

            viewModel.submit()
            advanceUntilIdle()

            assertEquals(SignInEvent.SignedIn(pendingStep = null), viewModel.events.first())
        }
    }

    @Test
    fun `the countdown shows minutes and seconds`() {
        assertEquals("15:00", formatCountdown(900))
        assertEquals("0:05", formatCountdown(5))
        assertEquals("1:00", formatCountdown(60))
    }

    private val session = Session("token-1", User("user-ana", "Ana Quispe", "ana.quispe@gmail.com"))

    private val accounts = listOf(
        GoogleAccount("google-ana", "Ana Quispe", "ana.quispe@gmail.com"),
        GoogleAccount("google-ana-work", "Ana Q. · Trabajo", "ana@reflowtech.pe"),
    )

    private inner class FakeRepository : AuthRepository {
        var signInAnswer: suspend () -> AuthOutcome<Session> = { Outcome.Success(session) }
        var googleAnswer: suspend () -> AuthOutcome<Session> = { Outcome.Success(session) }
        var accountsAnswer: suspend () -> AuthOutcome<List<GoogleAccount>> = { Outcome.Success(accounts) }
        val signIns = mutableListOf<Pair<String, String>>()
        val googleSignIns = mutableListOf<String>()
        val signInCalls get() = signIns.size
        var googleAccountCalls = 0

        override suspend fun signIn(contact: String, password: String): AuthOutcome<Session> {
            signIns += contact to password
            return signInAnswer()
        }

        override suspend fun googleAccounts(): AuthOutcome<List<GoogleAccount>> {
            googleAccountCalls++
            return accountsAnswer()
        }

        override suspend fun signInWithGoogle(accountId: String): AuthOutcome<Session> {
            googleSignIns += accountId
            return googleAnswer()
        }

        override suspend fun requestPasswordReset(contact: String): AuthOutcome<PasswordReset> = TODO()
        override suspend fun register(registration: Registration): AuthOutcome<VerificationChallenge> = TODO()
        override suspend fun verifyCode(challengeId: String, code: String): AuthOutcome<Session> = TODO()
        override suspend fun resendCode(challengeId: String): AuthOutcome<VerificationChallenge> = TODO()
    }

    private class FakeSessionStore : SessionStore {
        override val session = MutableStateFlow<Session?>(null)
        override suspend fun save(session: Session) {
            this.session.value = session
        }

        override suspend fun clear() {
            session.value = null
        }
    }
}
