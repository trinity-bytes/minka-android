package com.trinitybytes.minka.features.profile.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.GetOwnerProfile
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.OwnerProfile
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class OwnerProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeItemsRepository()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = OwnerProfileViewModel(GetOwnerProfile(repository), "ana")

    @Test fun `shows the owner and their active listings`() = runTest(dispatcher) {
        repository.profile = Outcome.Success(OwnerProfile(ANA, listOf(listing("a"))))
        val model = viewModel()
        assertTrue(model.state.value.loading)
        advanceUntilIdle()
        assertEquals(listOf("ownerProfile:ana"), repository.calls)
        assertFalse(model.state.value.loading)
        assertEquals("Ana Quispe", model.state.value.profile?.owner?.name)
        assertEquals("AQ", model.state.value.initials)
        assertEquals(listOf("a"), model.state.value.profile?.activeListings?.map { it.id })
    }

    @Test fun `member since is kept only when the account has a date`() = runTest(dispatcher) {
        val since = Instant.parse("2025-03-01T00:00:00Z")
        repository.profile = Outcome.Success(OwnerProfile(ANA.copy(memberSince = since), emptyList()))
        val model = viewModel()
        advanceUntilIdle()
        assertEquals(since, model.state.value.profile?.owner?.memberSince)
        repository.profile = Outcome.Success(OwnerProfile(ANA, emptyList()))
        model.refresh()
        advanceUntilIdle()
        assertNull(model.state.value.profile?.owner?.memberSince)
    }

    @Test fun `an unknown owner is reported as not found`() = runTest(dispatcher) {
        repository.profile = Outcome.Failure(ItemsError.NotFound)
        val model = viewModel()
        advanceUntilIdle()
        assertTrue(model.state.value.notFound)
        assertNull(model.state.value.profile)
    }

    @Test fun `offline can be retried`() = runTest(dispatcher) {
        repository.profile = Outcome.Failure(ItemsError.Offline)
        val model = viewModel()
        advanceUntilIdle()
        assertTrue(model.state.value.offline)
        repository.profile = Outcome.Success(OwnerProfile(ANA, emptyList()))
        model.refresh()
        advanceUntilIdle()
        assertFalse(model.state.value.offline)
        assertEquals("Ana Quispe", model.state.value.profile?.owner?.name)
    }
}
