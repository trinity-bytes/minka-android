package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import java.io.IOException
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Interests (1.19), notifications (1.20) and "Todo listo" (1.21). */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupStepsViewModelTest {

    companion object {
        private const val KEY_INTEREST_IDS = "interest_ids"
    }

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
        SavedStateHandle(),
    )

    private fun notificationsViewModel(progress: FakeProgressStore, api33: Boolean) = NotificationsViewModel(
        NotificationsNeedRequest(FakePermissionPolicy(notificationsNeedRequest = api33)),
        FinishNotificationsStep(AdvanceSetup(sessionStore, progress)),
    )

    @Test
    fun `interests restore and persist`() = runTest(dispatcher) {
        val progress = progressAt(SetupStep.Interests)
        val handle = SavedStateHandle(mapOf(KEY_INTEREST_IDS to arrayListOf("libros")))
        val model = InterestsViewModel(
            GetInterests(repository),
            SaveInterests(sessionStore, repository, progress),
            SkipInterests(AdvanceSetup(sessionStore, progress)),
            handle,
        )
        advanceUntilIdle()
        assertEquals(setOf("libros"), model.uiState.value.selectedIds)
        model.onInterestToggled("plantas")
        assertEquals(setOf("libros", "plantas"), handle.get<ArrayList<String>>(KEY_INTEREST_IDS)!!.toSet())
    }

    @Test
    fun `failed progress write shows error without navigating`() = runTest(dispatcher) {
        val broken = object : OnboardingProgressStore {
            override val progress = kotlinx.coroutines.flow.MutableStateFlow<OnboardingProgress?>(null)
            override suspend fun save(progress: OnboardingProgress) {
                throw IOException()
            }
        }
        val interests = InterestsViewModel(
            GetInterests(repository),
            SaveInterests(sessionStore, repository, broken),
            SkipInterests(AdvanceSetup(sessionStore, broken)),
            SavedStateHandle(),
        )
        val notifications = NotificationsViewModel(
            NotificationsNeedRequest(FakePermissionPolicy(false)),
            FinishNotificationsStep(AdvanceSetup(sessionStore, broken)),
        )
        val allSet = AllSetViewModel(
            GetSetupSummary(sessionStore, repository),
            CompleteOnboarding(sessionStore, broken),
        )
        val interestEvents = mutableListOf<InterestsEvent>()
        val notificationEvents = mutableListOf<NotificationsEvent>()
        val allSetEvents = mutableListOf<AllSetEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { interests.events.toList(interestEvents) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            notifications.events.toList(notificationEvents)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { allSet.events.toList(allSetEvents) }
        advanceUntilIdle()
        interests.onSkip()
        notifications.onNotNow()
        allSet.onExplore()
        advanceUntilIdle()
        assertEquals(SetupMessage.Unexpected, interests.uiState.value.message)
        assertTrue(interestEvents.isEmpty())
        assertTrue(notificationEvents.isEmpty())
        assertTrue(allSetEvents.isEmpty())
        assertEquals(SetupMessage.Unexpected, notifications.uiState.value.message)
        assertEquals(SetupMessage.Unexpected, allSet.uiState.value.message)
        assertFalse(allSet.uiState.value.isFinishing)
        notifications.onMessageShown()
        allSet.onMessageShown()
        assertNull(notifications.uiState.value.message)
        assertNull(allSet.uiState.value.message)
    }

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
        assertEquals(setOf("libros", "plantas"), repository.savedInterests.single().toSet())
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
    fun `omitir twice skips once`() = runTest(dispatcher) {
        val viewModel = interestsViewModel(progressAt(SetupStep.Interests))
        advanceUntilIdle()
        val events = mutableListOf<InterestsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onSkip()
        viewModel.onSkip()
        viewModel.onInterestToggled("libros")
        viewModel.onContinue()
        advanceUntilIdle()

        assertTrue(repository.savedInterests.isEmpty())
        assertEquals(listOf<InterestsEvent>(InterestsEvent.Done), events)
    }

    @Test
    fun `offline keeps the chosen interests and offers reintentar in a snackbar`() = runTest(dispatcher) {
        repository.saveAnswer = { Outcome.Failure(ProfileError.Offline) }
        val progress = progressAt(SetupStep.Interests)
        val viewModel = interestsViewModel(progress)
        advanceUntilIdle()
        val events = mutableListOf<InterestsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        viewModel.onInterestToggled("libros")

        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals("1.22", SetupMessage.Offline, viewModel.uiState.value.message)
        assertNull("the chips stay", viewModel.uiState.value.loadError)
        assertEquals(setOf("libros"), viewModel.uiState.value.selectedIds)
        assertTrue(viewModel.uiState.value.canContinue)
        assertTrue(events.isEmpty())
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.Interests), progress.progress.value)
    }

    @Test
    fun `reintentar saves the interests chosen now, once`() = runTest(dispatcher) {
        repository.saveAnswer = { Outcome.Failure(ProfileError.Offline) }
        val progress = progressAt(SetupStep.Interests)
        val viewModel = interestsViewModel(progress)
        advanceUntilIdle()
        val events = mutableListOf<InterestsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        viewModel.onInterestToggled("libros")
        viewModel.onContinue()
        advanceUntilIdle()

        viewModel.onInterestToggled("plantas")
        repository.saveAnswer = { Outcome.Success(Unit) }
        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(listOf("libros", "plantas")), repository.savedInterests)
        assertNull(viewModel.uiState.value.message)
        assertEquals(listOf<InterestsEvent>(InterestsEvent.Done), events)
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.Notifications), progress.progress.value)
    }

    @Test
    fun `offline while loading the interests shows 1_22 and reintentar loads them`() = runTest(dispatcher) {
        repository.interestsAnswer = { Outcome.Failure(ProfileError.Offline) }
        val viewModel = interestsViewModel(progressAt(SetupStep.Interests))
        advanceUntilIdle()
        assertEquals(LoadError.Offline, viewModel.uiState.value.loadError)

        repository.interestsAnswer = { Outcome.Success(TEST_INTERESTS) }
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadError)
        assertEquals(TEST_INTERESTS, viewModel.uiState.value.interests)
    }

    @Test
    fun `any other failed load of the interests also shows the full-screen error`() = runTest(dispatcher) {
        repository.interestsAnswer = { Outcome.Failure(ProfileError.Unexpected) }
        val viewModel = interestsViewModel(progressAt(SetupStep.Interests))
        advanceUntilIdle()
        assertEquals(LoadError.Unexpected, viewModel.uiState.value.loadError)
        assertNull(viewModel.uiState.value.message)

        repository.interestsAnswer = { Outcome.Success(TEST_INTERESTS) }
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadError)
        assertEquals(TEST_INTERESTS, viewModel.uiState.value.interests)
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
