package com.trinitybytes.minka.features.profile.presentation

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.features.items.application.DeleteListing
import com.trinitybytes.minka.features.items.application.GetMyDrafts
import com.trinitybytes.minka.features.items.application.GetMyListings
import com.trinitybytes.minka.features.items.application.PauseListing
import com.trinitybytes.minka.features.items.application.ResumeListing
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ListingStatus
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyListingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeItemsRepository()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = MyListingsViewModel(
        GetMyListings(repository),
        GetMyDrafts(repository),
        PauseListing(repository),
        ResumeListing(repository),
        DeleteListing(repository),
        handle,
    )

    private fun seed() {
        repository.listings += listing("a")
        repository.listings += listing("b")
        repository.listings += listing("p", ListingStatus.PAUSED)
        repository.drafts += savedDraft("d")
    }

    @Test fun `loads active, paused and drafts and starts on Activas`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        assertTrue(model.state.value.loading)
        advanceUntilIdle()
        val state = model.state.value
        assertFalse(state.loading)
        assertEquals(MyListingsFilter.ACTIVE, state.filter)
        assertEquals(listOf("a", "b"), state.active.map { it.id })
        assertEquals(listOf("p"), state.paused.map { it.id })
        assertEquals(listOf("d"), state.drafts.map { it.id })
    }

    @Test fun `the chosen filter survives a recreated ViewModel`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        viewModel(handle).selectFilter(MyListingsFilter.DRAFTS)
        assertEquals(MyListingsFilter.DRAFTS, viewModel(handle).state.value.filter)
    }

    @Test fun `pausing moves the listing to Pausadas and announces it with an undo id`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        val events = mutableListOf<MyListingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.pause("a")
        advanceUntilIdle()
        assertEquals(listOf("b"), model.state.value.active.map { it.id })
        assertEquals(listOf("p", "a").sorted(), model.state.value.paused.map { it.id }.sorted())
        assertEquals(listOf<MyListingsEvent>(MyListingsEvent.Paused("a")), events)
    }

    @Test fun `undoing a pause makes the listing active again without another message`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        model.pause("a")
        advanceUntilIdle()
        val events = mutableListOf<MyListingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        events.clear()
        model.undoPause("a")
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), model.state.value.active.map { it.id }.sorted())
        assertTrue(events.isEmpty())
    }

    @Test fun `resuming a paused listing announces it`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        val events = mutableListOf<MyListingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.resume("p")
        advanceUntilIdle()
        assertTrue(model.state.value.paused.isEmpty())
        assertEquals(listOf<MyListingsEvent>(MyListingsEvent.Resumed), events)
    }

    @Test fun `deleting asks first and removes nothing until confirmed`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        model.requestDelete("a")
        assertEquals("a", model.state.value.pendingDelete)
        assertTrue(repository.calls.none { it.startsWith("delete") })
        model.cancelDelete()
        assertNull(model.state.value.pendingDelete)
        assertEquals(2, model.state.value.active.size)
    }

    @Test fun `confirming the deletion removes the listing and announces it`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        val events = mutableListOf<MyListingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.requestDelete("a")
        model.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf("delete:a"), repository.calls)
        assertEquals(listOf("b"), model.state.value.active.map { it.id })
        assertNull(model.state.value.pendingDelete)
        assertEquals(listOf<MyListingsEvent>(MyListingsEvent.Deleted), events)
    }

    @Test fun `a draft can be deleted the same way`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        model.requestDelete("d")
        model.confirmDelete()
        advanceUntilIdle()
        assertTrue(model.state.value.drafts.isEmpty())
    }

    @Test fun `a failed pause keeps the list and reports the failure`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        repository.failure = ItemsError.Offline
        val events = mutableListOf<MyListingsEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.toList(events) }
        model.pause("a")
        advanceUntilIdle()
        assertEquals(2, model.state.value.active.size)
        assertEquals(listOf<MyListingsEvent>(MyListingsEvent.Failed), events)
    }

    @Test fun `when nothing loads the screen reports the error and retry recovers`() = runTest(dispatcher) {
        seed()
        repository.failure = ItemsError.Offline
        val model = viewModel()
        advanceUntilIdle()
        assertTrue(model.state.value.error)
        assertTrue(model.state.value.offline)
        assertFalse(model.state.value.loading)
        repository.failure = null
        model.refresh()
        advanceUntilIdle()
        assertFalse(model.state.value.error)
        assertEquals(2, model.state.value.active.size)
    }

    @Test fun `cached listings stay visible and flag the offline state`() = runTest(dispatcher) {
        seed()
        repository.fromCache = true
        val model = viewModel()
        advanceUntilIdle()
        assertTrue(model.state.value.offline)
        assertFalse(model.state.value.error)
        assertEquals(2, model.state.value.active.size)
    }

    @Test fun `selecting a listing remembers it for the detail pane`() = runTest(dispatcher) {
        seed()
        val model = viewModel()
        advanceUntilIdle()
        model.select("b")
        assertEquals("b", model.state.value.selectedId)
    }
}
