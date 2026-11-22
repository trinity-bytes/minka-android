package com.trinitybytes.minka.features.onboarding.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.ANA
import com.trinitybytes.minka.features.onboarding.BARRANCO
import com.trinitybytes.minka.features.onboarding.FakeLocationProvider
import com.trinitybytes.minka.features.onboarding.FakeProfileRepository
import com.trinitybytes.minka.features.onboarding.FakeProgressStore
import com.trinitybytes.minka.features.onboarding.SAN_ISIDRO
import com.trinitybytes.minka.features.onboarding.TEST_DISTRICTS
import com.trinitybytes.minka.features.onboarding.application.ChooseDistrict
import com.trinitybytes.minka.features.onboarding.application.GetDistricts
import com.trinitybytes.minka.features.onboarding.application.LocateDistrict
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import com.trinitybytes.minka.features.onboarding.signedInSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DistrictViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeProfileRepository()
    private val location = FakeLocationProvider()
    private val progress = FakeProgressStore(OnboardingProgress.Pending(ANA.user.id, SetupStep.District))
    private val sessionStore = signedInSessionStore()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DistrictViewModel(
        GetDistricts(repository),
        LocateDistrict(location, repository),
        ChooseDistrict(sessionStore, repository, progress),
    )

    private val DistrictViewModel.state get() = uiState.value

    @Test
    fun `continuar is disabled until a district is chosen`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(TEST_DISTRICTS, viewModel.state.visibleDistricts)
        assertFalse("1.16: Sin selección no se avanza", viewModel.state.canContinue)

        viewModel.onContinue()
        advanceUntilIdle()
        assertTrue(repository.savedDistricts.isEmpty())

        viewModel.onDistrictSelected(SAN_ISIDRO.id)
        assertEquals(SAN_ISIDRO.id, viewModel.state.selectedId)
        assertTrue(viewModel.state.canContinue)
    }

    @Test
    fun `the search filters the list ignoring case and accents but keeps the choice`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onDistrictSelected(SAN_ISIDRO.id)

        viewModel.onQueryChange("BARRANCÓ")
        assertEquals(listOf(BARRANCO), viewModel.state.visibleDistricts)
        assertEquals(SAN_ISIDRO.id, viewModel.state.selectedId)
        assertTrue(viewModel.state.canContinue)
    }

    @Test
    fun `usar mi ubicacion asks for the permission and the approximate location preselects a district`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()
            val events = mutableListOf<DistrictEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

            viewModel.onUseMyLocation()
            advanceUntilIdle()
            assertEquals(listOf<DistrictEvent>(DistrictEvent.RequestLocationPermission), events)
            assertEquals("nothing is read before the permission", 0, location.calls)

            viewModel.onLocationPermissionResult(granted = true)
            advanceUntilIdle()
            assertEquals(listOf(location.location), repository.locatedFrom)
            assertEquals(BARRANCO.id, viewModel.state.selectedId)
            assertFalse(viewModel.state.isLocating)
            assertTrue(viewModel.state.canContinue)
            assertTrue("preselecting does not save yet", repository.savedDistricts.isEmpty())
        }

    @Test
    fun `a denied location shows the settings snackbar and keeps the manual choice`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onDistrictSelected(SAN_ISIDRO.id)

        viewModel.onUseMyLocation()
        viewModel.onLocationPermissionResult(granted = false)
        advanceUntilIdle()

        assertEquals(SetupMessage.LocationDenied, viewModel.state.message)
        assertEquals(0, location.calls)
        assertEquals(SAN_ISIDRO.id, viewModel.state.selectedId)
        assertEquals(TEST_DISTRICTS, viewModel.state.visibleDistricts)
        assertTrue(viewModel.state.canContinue)

        viewModel.onMessageShown()
        assertNull(viewModel.state.message)
    }

    @Test
    fun `continuar saves the district, moves the setup to the interests and navigates`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onDistrictSelected(SAN_ISIDRO.id)

        viewModel.onContinue()
        assertTrue(viewModel.state.isSaving)
        assertFalse(viewModel.state.canContinue)
        advanceUntilIdle()

        assertEquals(listOf(ANA.user.id to SAN_ISIDRO.id), repository.savedDistricts)
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.Interests), progress.progress.value)
        assertEquals(DistrictEvent.Chosen, viewModel.events.first())
        assertFalse(viewModel.state.isSaving)
    }

    @Test
    fun `offline keeps the selection and the step and offers reintentar in a snackbar`() = runTest(dispatcher) {
        repository.saveAnswer = { Outcome.Failure(ProfileError.Offline) }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onQueryChange("san")
        viewModel.onDistrictSelected(SAN_ISIDRO.id)
        val events = mutableListOf<DistrictEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onContinue()
        advanceUntilIdle()

        assertEquals("1.22", SetupMessage.Offline, viewModel.state.message)
        assertNull("the list stays", viewModel.state.loadError)
        assertEquals(SAN_ISIDRO.id, viewModel.state.selectedId)
        assertEquals("san", viewModel.state.query)
        assertTrue(viewModel.state.canContinue)
        assertTrue(events.isEmpty())
        assertEquals(OnboardingProgress.Pending(ANA.user.id, SetupStep.District), progress.progress.value)
    }

    @Test
    fun `reintentar saves the district chosen now`() = runTest(dispatcher) {
        repository.saveAnswer = { Outcome.Failure(ProfileError.Offline) }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onDistrictSelected(SAN_ISIDRO.id)
        val events = mutableListOf<DistrictEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        viewModel.onContinue()
        advanceUntilIdle()

        viewModel.onDistrictSelected(BARRANCO.id)
        repository.saveAnswer = { Outcome.Success(Unit) }
        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(ANA.user.id to BARRANCO.id), repository.savedDistricts)
        assertNull(viewModel.state.message)
        assertEquals(listOf<DistrictEvent>(DistrictEvent.Chosen), events)
    }

    @Test
    fun `offline while locating offers reintentar, which locates again`() = runTest(dispatcher) {
        repository.nearestAnswer = { Outcome.Failure(ProfileError.Offline) }
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onLocationPermissionResult(granted = true)
        advanceUntilIdle()
        assertEquals(SetupMessage.Offline, viewModel.state.message)

        repository.nearestAnswer = { Outcome.Success(BARRANCO) }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(BARRANCO.id, viewModel.state.selectedId)
        assertEquals(2, location.calls)
    }

    @Test
    fun `offline while loading the districts shows 1_22 and reintentar loads them`() = runTest(dispatcher) {
        repository.districtsAnswer = { Outcome.Failure(ProfileError.Offline) }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(LoadError.Offline, viewModel.state.loadError)
        assertNull(viewModel.state.message)

        repository.districtsAnswer = { Outcome.Success(TEST_DISTRICTS) }
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.state.loadError)
        assertFalse(viewModel.state.isLoading)
        assertEquals(TEST_DISTRICTS, viewModel.state.visibleDistricts)
    }

    @Test
    fun `any other failed load also shows the full-screen error with reintentar`() = runTest(dispatcher) {
        repository.districtsAnswer = { Outcome.Failure(ProfileError.Unexpected) }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(LoadError.Unexpected, viewModel.state.loadError)
        assertNull("no snackbar on top of the error state", viewModel.state.message)

        repository.districtsAnswer = { Outcome.Success(TEST_DISTRICTS) }
        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.state.loadError)
        assertEquals(TEST_DISTRICTS, viewModel.state.visibleDistricts)
    }
}
