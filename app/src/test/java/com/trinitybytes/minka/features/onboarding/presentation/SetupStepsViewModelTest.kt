package com.trinitybytes.minka.features.onboarding.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.ANA
import com.trinitybytes.minka.features.onboarding.FakePermissionPolicy
import com.trinitybytes.minka.features.onboarding.FakeProfileRepository
import com.trinitybytes.minka.features.onboarding.FakeProgressStore
import com.trinitybytes.minka.features.onboarding.TEST_INTERESTS
import com.trinitybytes.minka.features.onboarding.application.AdvanceSetup
import com.trinitybytes.minka.features.onboarding.application.CompleteOnboarding
import com.trinitybytes.minka.features.onboarding.application.FinishNotificationsStep
import com.trinitybytes.minka.features.onboarding.application.GetInterests
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
import com.trinitybytes.minka.features.onboarding.application.NotificationsNeedRequest
import com.trinitybytes.minka.features.onboarding.application.SaveInterests
import com.trinitybytes.minka.features.onboarding.application.SkipInterests
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import com.trinitybytes.minka.features.onboarding.signedInSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Interests (1.19), notifications (1.20) and "Todo listo" (1.21). */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupStepsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeProfileRepository()
    private val sessionStore = signedInSessionStore()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun progressAt(step: SetupStep) = FakeProgressStore(OnboardingProgress.Pending(ANA.user.id, step))

    private fun interestsViewModel(progress: FakeProgressStore) = InterestsViewModel(
        GetInterests(repository),
        SaveInterests(sessionStore, repository, progress),
        SkipInterests(AdvanceSetup(sessionStore, progress)),
    )

    private fun notificationsViewModel(progress: FakeProgressStore, api33: Boolean) = NotificationsViewModel(
        NotificationsNeedRequest(FakePermissionPolicy(notificationsNeedRequest = api33)),
        FinishNotificationsStep(AdvanceSetup(sessionStore, progress)),
    )

    @Test
    fun `continuar waits for one interest and saves the chosen ones`() = runTest(dispatcher) {
        val progress = progressAt(SetupStep.Interests)
        val viewModel = interestsViewModel(progress)
        advanceUntilIdle()
        val events = mutableListOf<InterestsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        assertEquals(TEST_INTERESTS, viewModel.uiState.value.interests)
        assertFalse(viewModel.uiState.value.canContinue)

        viewModel.onInterestToggled("libros")
        viewModel.onInterestToggled("plantas")
        viewModel.onInterestToggled("hogar")
        viewModel.onInterestToggled("hogar")
        assertEquals(setOf("libros", "plantas"), viewModel.uiState.value.selectedIds)
        assertTrue(viewModel.uiState.value.canContinue)

        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(1, repository.savedInterests.size)
        assertEquals(ANA.user.id, repository.savedInterests.single().first)
        assertEquals(setOf("libros", "plantas"), repository.savedInterests.single().second.toSet())
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.Notifications), progress.progress.value)
        assertEquals(listOf<InterestsEvent>(InterestsEvent.Done), events)
    }

    @Test
    fun `omitir skips the interests without saving any`() = runTest(dispatcher) {
        val progress = progressAt(SetupStep.Interests)
        val viewModel = interestsViewModel(progress)
        advanceUntilIdle()
        val events = mutableListOf<InterestsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        viewModel.onInterestToggled("libros")

        viewModel.onSkip()
        advanceUntilIdle()

        assertTrue(repository.savedInterests.isEmpty())
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.Notifications), progress.progress.value)
        assertEquals(listOf<InterestsEvent>(InterestsEvent.Done), events)
    }

    @Test
    fun `offline keeps the chosen interests`() = runTest(dispatcher) {
        repository.saveAnswer = { Outcome.Failure(ProfileError.Offline) }
        val progress = progressAt(SetupStep.Interests)
        val viewModel = interestsViewModel(progress)
        advanceUntilIdle()
        val events = mutableListOf<InterestsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        viewModel.onInterestToggled("libros")

        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(SetupMessage.Offline, viewModel.uiState.value.message)
        assertEquals(setOf("libros"), viewModel.uiState.value.selectedIds)
        assertTrue(viewModel.uiState.value.canContinue)
        assertTrue(events.isEmpty())
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.Interests), progress.progress.value)
    }

    @Test
    fun `on Android 13+ activar asks the system and a denial still continues`() = runTest(dispatcher) {
        val progress = progressAt(SetupStep.Notifications)
        val viewModel = notificationsViewModel(progress, api33 = true)
        val events = mutableListOf<NotificationsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onEnable()
        advanceUntilIdle()
        assertEquals(listOf<NotificationsEvent>(NotificationsEvent.RequestPermission), events)
        assertEquals("waits for the answer", OnboardingProgress.Pending(ANA.user.id, SetupStep.Notifications), progress.progress.value)

        viewModel.onPermissionResult(granted = false)
        advanceUntilIdle()
        assertEquals(listOf(NotificationsEvent.RequestPermission, NotificationsEvent.Done), events)
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.AllSet), progress.progress.value)
    }

    @Test
    fun `below Android 13 activar continues without the system dialog`() = runTest(dispatcher) {
        val progress = progressAt(SetupStep.Notifications)
        val viewModel = notificationsViewModel(progress, api33 = false)
        val events = mutableListOf<NotificationsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onEnable()
        advanceUntilIdle()

        assertEquals(listOf<NotificationsEvent>(NotificationsEvent.Done), events)
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.AllSet), progress.progress.value)
    }

    @Test
    fun `ahora no continues without asking`() = runTest(dispatcher) {
        val progress = progressAt(SetupStep.Notifications)
        val viewModel = notificationsViewModel(progress, api33 = true)
        val events = mutableListOf<NotificationsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onNotNow()
        advanceUntilIdle()

        assertEquals(listOf<NotificationsEvent>(NotificationsEvent.Done), events)
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.AllSet), progress.progress.value)
    }

    @Test
    fun `todo listo greets by first name, marks onboarding complete and goes home`() = runTest(dispatcher) {
        val progress = progressAt(SetupStep.AllSet)
        val viewModel = AllSetViewModel(
            GetSetupSummary(sessionStore, repository),
            CompleteOnboarding(sessionStore, progress),
        )
        advanceUntilIdle()
        val events = mutableListOf<AllSetEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        assertEquals(AllSetUiState(firstName = "Ana", districtName = "Miraflores"), viewModel.uiState.value)

        viewModel.onExplore()
        advanceUntilIdle()

        assertEquals(OnboardingProgress.Completed(ANA.user.id), progress.progress.value)
        assertEquals(listOf<AllSetEvent>(AllSetEvent.GoHome), events)
    }
}
