package com.trinitybytes.minka.features.profile.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.ObserveSession
import com.trinitybytes.minka.features.onboarding.FakeProfileRepository
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import com.trinitybytes.minka.features.onboarding.signedInSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = signedInSessionStore()
    private val profiles = FakeProfileRepository()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ProfileViewModel(ObserveSession(sessions), GetSetupSummary(sessions, profiles))

    @Test fun `shows the name, initials and district of the signed-in user`() = runTest(dispatcher) {
        val model = viewModel()
        advanceUntilIdle()
        assertEquals(ProfileUiState(name = "Ana Quispe", initials = "AQ", district = "Miraflores"), model.state.value)
    }

    @Test fun `without a known district the line is left out`() = runTest(dispatcher) {
        profiles.profileAnswer = { Outcome.Failure(ProfileError.Offline) }
        val model = viewModel()
        advanceUntilIdle()
        assertNull(model.state.value.district)
        assertEquals("Ana Quispe", model.state.value.name)
    }
}
