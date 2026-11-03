package com.trinitybytes.minka.features.onboarding.presentation

import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.onboarding.FakeProgressStore
import com.trinitybytes.minka.features.onboarding.application.ResolveLaunch
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val session = Session("token-1", User("user-ana", "Ana Quispe", "ana.quispe@gmail.com"))

    private fun viewModel(sessions: Flow<Session?>, progress: OnboardingProgress? = null) =
        SplashViewModel(ResolveLaunch(FakeSessionStore(sessions), FakeProgressStore(progress)))

    @Test
    fun `a signed-in user with the setup unfinished goes back to the pending step`() = runTest(dispatcher) {
        SetupStep.entries.forEach { step ->
            val viewModel = viewModel(MutableStateFlow(session), OnboardingProgress.Pending("user-ana", step))

            advanceUntilIdle()

            assertEquals(SplashUiState.Ready(SplashDestination.Setup(step)), viewModel.uiState.value)
        }
    }

    @Test
    fun `a fully onboarded user goes home`() = runTest(dispatcher) {
        val viewModel = viewModel(MutableStateFlow(session), OnboardingProgress.Completed("user-ana"))

        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Home), viewModel.uiState.value)
    }

    @Test
    fun `a signed-out user goes to welcome even with a setup left pending`() = runTest(dispatcher) {
        val viewModel = viewModel(MutableStateFlow(null), OnboardingProgress.Pending("user-ana", SetupStep.Interests))

        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Welcome), viewModel.uiState.value)
    }

    @Test
    fun `a pending setup of another account does not apply to the signed-in one`() = runTest(dispatcher) {
        val viewModel = viewModel(MutableStateFlow(session), OnboardingProgress.Pending("user-luis", SetupStep.District))

        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Home), viewModel.uiState.value)
    }

    @Test
    fun `a restored session leads to home`() = runTest(dispatcher) {
        val viewModel = viewModel(MutableStateFlow(session))

        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Home), viewModel.uiState.value)
    }

    @Test
    fun `no session leads to welcome`() = runTest(dispatcher) {
        val viewModel = viewModel(MutableStateFlow(null))

        advanceUntilIdle()

        assertEquals(SplashUiState.Ready(SplashDestination.Welcome), viewModel.uiState.value)
    }

    @Test
    fun `the splash keeps loading while the session is being restored`() = runTest(dispatcher) {
        val pending = MutableSharedFlow<Session?>()
        val viewModel = viewModel(pending)

        advanceUntilIdle()
        assertEquals(SplashUiState.Restoring, viewModel.uiState.value)

        pending.emit(null)
        advanceUntilIdle()
        assertEquals(SplashUiState.Ready(SplashDestination.Welcome), viewModel.uiState.value)
    }

    private class FakeSessionStore(override val session: Flow<Session?>) : SessionStore {
        override suspend fun save(session: Session) = Unit
        override suspend fun clear() = Unit
    }
}
