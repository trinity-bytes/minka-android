package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.navigation.VerifyCodeRoute
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.ResendCode
import com.trinitybytes.minka.features.auth.application.VerifyRegistration
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.onboarding.FakeProgressStore
import com.trinitybytes.minka.features.onboarding.application.BeginOnboarding
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.IOException
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyCodeViewModelTest {

    companion object {
        private const val KEY_ATTEMPTS = "verify_attempts"
        private const val KEY_DEADLINE = "verify_deadline"
    }

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRegistrationRepository()
    private val sessionStore = FakeSessionStore()
    private val progressStore = FakeProgressStore()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(resendAfterSeconds: Int = 60, sessions: SessionStore = sessionStore) = VerifyCodeViewModel(
        SavedStateHandle(
            mapOf(
                VerifyCodeViewModel.ARG_CHALLENGE_ID to "challenge-1",
                VerifyCodeViewModel.ARG_DESTINATION to "luis.mamani@gmail.com",
                VerifyCodeViewModel.ARG_ATTEMPTS_LEFT to 5,
                VerifyCodeViewModel.ARG_RESEND_AFTER_SECONDS to resendAfterSeconds,
            ),
        ),
        VerifyRegistration(repository, sessions, BeginOnboarding(progressStore)),
        ResendCode(repository),
        object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(dispatcher.scheduler.currentTime)
        },
    )

    private val VerifyCodeViewModel.state get() = uiState.value

    private fun wrongCodes() {
        repository.verifyAnswer = { Outcome.Failure(AuthError.WrongCode(attemptsLeft = 4)) }
    }

    @Test
    fun `route arguments populate saved state and preserve restored progress`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf<String, Any>(KEY_ATTEMPTS to 2, KEY_DEADLINE to 20_000L))
        val route = VerifyCodeRoute("challenge-route", "route@example.com", 5, 60)
        val model = VerifyCodeViewModel(
            handle,
            VerifyRegistration(repository, sessionStore, BeginOnboarding(progressStore)),
            ResendCode(repository),
            TEST_CLOCK,
            route,
        )
        assertEquals("challenge-route", handle.get<String>(VerifyCodeViewModel.ARG_CHALLENGE_ID))
        assertEquals("route@example.com", handle.get<String>(VerifyCodeViewModel.ARG_DESTINATION))
        assertEquals(5, handle.get<Int>(VerifyCodeViewModel.ARG_ATTEMPTS_LEFT))
        assertEquals(60, handle.get<Int>(VerifyCodeViewModel.ARG_RESEND_AFTER_SECONDS))
        assertEquals("route@example.com", model.state.destination)
        assertEquals(2, model.state.attemptsLeft)
        assertEquals(20_000L, handle.get<Long>(KEY_DEADLINE))
    }

    @Test
    fun `expired attempts restore exhausted and resend persists a new deadline`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf(
            VerifyCodeViewModel.ARG_CHALLENGE_ID to "challenge-1",
            VerifyCodeViewModel.ARG_DESTINATION to "ana@example.com",
            VerifyCodeViewModel.ARG_ATTEMPTS_LEFT to 5,
            VerifyCodeViewModel.ARG_RESEND_AFTER_SECONDS to 60,
            KEY_ATTEMPTS to 0,
            KEY_DEADLINE to -1L,
        ))
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(dispatcher.scheduler.currentTime)
        }
        val model = VerifyCodeViewModel(
            handle,
            VerifyRegistration(repository, sessionStore, BeginOnboarding(progressStore)),
            ResendCode(repository),
            clock,
        )
        assertEquals(CodeError.Exhausted, model.state.codeError)
        assertNull(model.state.resendSecondsLeft)
        model.onCodeChange("123456")
        assertFalse(model.state.canVerify)
        model.resend()
        runCurrent()
        assertEquals(5, handle.get<Int>(KEY_ATTEMPTS))
        assertEquals(60_000L, handle.get<Long>(KEY_DEADLINE))
        assertNull(model.state.codeError)
    }

    @Test
    fun `failed beginning setup shows unexpected without saving session or navigation`() = runTest(dispatcher) {
        val broken = object : OnboardingProgressStore {
            override val progress = MutableStateFlow<OnboardingProgress?>(null)
            override suspend fun save(progress: OnboardingProgress) {
                throw IOException()
            }
        }
        val handle = SavedStateHandle(mapOf(
            VerifyCodeViewModel.ARG_CHALLENGE_ID to "challenge-1",
            VerifyCodeViewModel.ARG_DESTINATION to "ana@example.com",
            VerifyCodeViewModel.ARG_ATTEMPTS_LEFT to 5,
            VerifyCodeViewModel.ARG_RESEND_AFTER_SECONDS to 0,
        ))
        val model = VerifyCodeViewModel(
            handle,
            VerifyRegistration(repository, sessionStore, BeginOnboarding(broken)),
            ResendCode(repository),
            TEST_CLOCK,
        )
        val events = mutableListOf<VerifyEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.onCodeChange("482915")
        model.verify()
        advanceUntilIdle()
        assertEquals(VerifyMessage.Unexpected, model.state.message)
        assertNull(sessionStore.session.value)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `attempts and absolute deadline restore while code stays private`() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf(
            VerifyCodeViewModel.ARG_CHALLENGE_ID to "challenge-1",
            VerifyCodeViewModel.ARG_DESTINATION to "ana@example.com",
            VerifyCodeViewModel.ARG_ATTEMPTS_LEFT to 5,
            VerifyCodeViewModel.ARG_RESEND_AFTER_SECONDS to 60,
            KEY_ATTEMPTS to 2,
            KEY_DEADLINE to 20_000L,
        ))
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("UTC")
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = Instant.ofEpochMilli(dispatcher.scheduler.currentTime)
        }
        val model = VerifyCodeViewModel(
            handle,
            VerifyRegistration(repository, sessionStore, BeginOnboarding(progressStore)),
            ResendCode(repository),
            clock,
        )
        assertEquals(2, model.state.attemptsLeft)
        assertEquals(20L, model.state.resendSecondsLeft)
        model.onCodeChange("123456")
        repository.verifyAnswer = { Outcome.Failure(AuthError.WrongCode(1)) }
        model.verify()
        runCurrent()
        assertEquals(1, handle.get<Int>(KEY_ATTEMPTS))
        assertFalse(handle.keys().any { it == "code" || it == "verify_code" })
        advanceTimeBy(21_000)
        runCurrent()
        assertNull(model.state.resendSecondsLeft)
        assertEquals(20_000L, handle.get<Long>(KEY_DEADLINE))
    }

    @Test
    fun `the code takes 6 digits and verificar waits for all of them`() {
        val viewModel = viewModel()
        assertEquals("luis.mamani@gmail.com", viewModel.state.destination)
        assertEquals(5, viewModel.state.attemptsLeft)
        assertFalse(viewModel.state.canVerify)

        viewModel.onCodeChange("48291")
        assertFalse(viewModel.state.canVerify)
        viewModel.onCodeChange("4a8-2 9.15")
        assertEquals("only digits are kept", "482915", viewModel.state.code)
        assertTrue(viewModel.state.canVerify)
        viewModel.onCodeChange("4829157")
        assertEquals("no more than 6", "482915", viewModel.state.code)
    }

    @Test
    fun `verifying the code saves the session and moves on`() = runTest(dispatcher) {
        val pending = CompletableDeferred<AuthOutcome<Session>>()
        repository.verifyAnswer = { pending.await() }
        val viewModel = viewModel()
        viewModel.onCodeChange("482915")

        viewModel.verify()
        runCurrent()
        assertTrue(viewModel.state.isLoading)
        assertFalse(viewModel.state.canVerify)
        assertNull("not saved before the answer", sessionStore.session.value)

        pending.complete(Outcome.Success(TEST_SESSION))
        runCurrent()

        assertEquals(listOf("challenge-1" to "482915"), repository.verifications)
        assertEquals(TEST_SESSION, sessionStore.session.value)
        assertEquals(VerifyEvent.Verified, viewModel.events.first())
    }

    @Test
    fun `a verified account starts the profile setup at the district step`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onCodeChange("482915")

        viewModel.verify()
        advanceUntilIdle()

        assertEquals(OnboardingProgress.Pending(TEST_SESSION.user.id, SetupStep.District), progressStore.progress.value)
        assertEquals(VerifyEvent.Verified, viewModel.events.first())
    }

    @Test
    fun `the pending setup is saved before the session so a crash in between never skips it`() = runTest(dispatcher) {
        var progressWhenSignedIn: OnboardingProgress? = null
        val recordingSessions = object : SessionStore {
            override val session = MutableStateFlow<Session?>(null)
            override suspend fun save(session: Session) {
                progressWhenSignedIn = progressStore.progress.value
                this.session.value = session
            }
            override suspend fun clear() = Unit
        }
        val viewModel = viewModel(sessions = recordingSessions)
        viewModel.onCodeChange("482915")

        viewModel.verify()
        advanceUntilIdle()

        assertEquals(OnboardingProgress.Pending(TEST_SESSION.user.id, SetupStep.District), progressWhenSignedIn)
        assertEquals(TEST_SESSION, recordingSessions.session.value)
        assertEquals(VerifyEvent.Verified, viewModel.events.first())
    }

    @Test
    fun `a wrong code does not start the profile setup`() = runTest(dispatcher) {
        wrongCodes()
        val viewModel = viewModel()
        viewModel.onCodeChange("111111")

        viewModel.verify()
        advanceUntilIdle()

        assertNull(progressStore.progress.value)
    }

    @Test
    fun `an incomplete code is not sent`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onCodeChange("4829")

        viewModel.verify()
        advanceUntilIdle()

        assertTrue(repository.verifications.isEmpty())
    }

    @Test
    fun `a wrong code counts the attempts and keeps the digits until they are edited`() = runTest(dispatcher) {
        wrongCodes()
        val viewModel = viewModel()
        viewModel.onCodeChange("482915")

        viewModel.verify()
        advanceUntilIdle()

        assertEquals(CodeError.Wrong(attemptsLeft = 4), viewModel.state.codeError)
        assertEquals(4, viewModel.state.attemptsLeft)
        assertEquals("482915", viewModel.state.code)
        assertFalse("1.15: verificar waits for a new code", viewModel.state.canVerify)
        assertNull(sessionStore.session.value)

        repository.verifyAnswer = { Outcome.Failure(AuthError.WrongCode(attemptsLeft = 3)) }
        viewModel.onCodeChange("48291")
        assertNull(viewModel.state.codeError)
        viewModel.onCodeChange("482916")
        viewModel.verify()
        advanceUntilIdle()
        assertEquals(CodeError.Wrong(attemptsLeft = 3), viewModel.state.codeError)
        assertEquals(3, viewModel.state.attemptsLeft)
    }

    @Test
    fun `after 5 attempts a new code is required`() = runTest(dispatcher) {
        repository.verifyAnswer = { Outcome.Failure(AuthError.CodeExpired) }
        val viewModel = viewModel(resendAfterSeconds = 0)
        viewModel.onCodeChange("111111")

        viewModel.verify()
        advanceUntilIdle()

        assertEquals(CodeError.Exhausted, viewModel.state.codeError)
        assertEquals(0, viewModel.state.attemptsLeft)
        viewModel.onCodeChange("482915")
        assertEquals("editing does not lift it", CodeError.Exhausted, viewModel.state.codeError)
        assertFalse(viewModel.state.canVerify)
        viewModel.verify()
        advanceUntilIdle()
        assertEquals(1, repository.verifications.size)

        assertTrue(viewModel.state.canResend)
        viewModel.resend()
        advanceUntilIdle()

        assertEquals(listOf("challenge-1"), repository.resends)
        assertNull(viewModel.state.codeError)
        assertEquals(5, viewModel.state.attemptsLeft)
        assertEquals("", viewModel.state.code)
        viewModel.onCodeChange("482915")
        assertTrue(viewModel.state.canVerify)
    }

    @Test
    fun `reenviar codigo is enabled only after its countdown`() = runTest(dispatcher) {
        val viewModel = viewModel(resendAfterSeconds = 60)
        runCurrent()
        assertEquals(60L, viewModel.state.resendSecondsLeft)
        assertFalse(viewModel.state.canResend)
        viewModel.resend()
        runCurrent()
        assertTrue(repository.resends.isEmpty())

        advanceTimeBy(32_001)
        assertEquals("0:28", formatCountdown(viewModel.state.resendSecondsLeft!!))
        assertFalse(viewModel.state.canResend)

        advanceTimeBy(28_000)
        assertNull(viewModel.state.resendSecondsLeft)
        assertTrue(viewModel.state.canResend)

        viewModel.resend()
        runCurrent()
        assertEquals(listOf("challenge-1"), repository.resends)
        assertEquals("the countdown starts again", 60L, viewModel.state.resendSecondsLeft)
        assertFalse(viewModel.state.canResend)
    }

    @Test
    fun `a resend refused as too soon restarts the countdown with the server wait`() = runTest(dispatcher) {
        repository.resendAnswer = { Outcome.Failure(AuthError.TooSoon(retryAfterSeconds = 12)) }
        val viewModel = viewModel(resendAfterSeconds = 0)
        runCurrent()
        assertTrue(viewModel.state.canResend)

        viewModel.resend()
        runCurrent()

        assertEquals(12L, viewModel.state.resendSecondsLeft)
        assertFalse(viewModel.state.canResend)
        assertNull(viewModel.state.message)
    }

    @Test
    fun `offline keeps the typed code and offers reintentar in a snackbar`() = runTest(dispatcher) {
        repository.verifyAnswer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.onCodeChange("482915")

        viewModel.verify()
        advanceUntilIdle()

        assertEquals("1.22", VerifyMessage.Offline, viewModel.state.message)
        assertEquals("482915", viewModel.state.code)
        assertNull(viewModel.state.codeError)
        assertEquals(5, viewModel.state.attemptsLeft)
        assertTrue(viewModel.state.canVerify)

        repository.verifyAnswer = { Outcome.Failure(AuthError.Unexpected) }
        viewModel.verify()
        advanceUntilIdle()
        assertEquals(VerifyMessage.Unexpected, viewModel.state.message)
    }

    @Test
    fun `reintentar verifies the code the boxes hold now, once`() = runTest(dispatcher) {
        repository.verifyAnswer = { Outcome.Failure(AuthError.Offline) }
        val viewModel = viewModel()
        viewModel.onCodeChange("482914")
        viewModel.verify()
        advanceUntilIdle()

        viewModel.onCodeChange("482915")
        repository.verifyAnswer = { Outcome.Success(TEST_SESSION) }
        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf("challenge-1" to "482914", "challenge-1" to "482915"), repository.verifications)
        assertNull(viewModel.state.message)
        assertEquals(VerifyEvent.Verified, viewModel.events.first())
    }
}
