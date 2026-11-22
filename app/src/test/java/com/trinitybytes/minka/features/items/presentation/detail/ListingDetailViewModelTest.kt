package com.trinitybytes.minka.features.items.presentation.detail

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.ObserveSession
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.items.application.DeleteListing
import com.trinitybytes.minka.features.items.application.GetListingDetail
import com.trinitybytes.minka.features.items.application.HideListing
import com.trinitybytes.minka.features.items.application.PauseListing
import com.trinitybytes.minka.features.items.application.ResumeListing
import com.trinitybytes.minka.features.items.application.UndoHideListing
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ItemsRepository
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.Loaded
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import java.lang.reflect.Proxy
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val calls = mutableListOf<String>()
    private val results = mutableMapOf<String, Any>()
    private var signedInId: String? = "viewer"

    private val repository = Proxy.newProxyInstance(
        ItemsRepository::class.java.classLoader,
        arrayOf(ItemsRepository::class.java),
    ) { _, method, args ->
        calls += method.name + (args?.firstOrNull()?.let { ":$it" } ?: "")
        results[method.name] ?: error("No result set for ${method.name}")
    } as ItemsRepository

    private val sessions = object : SessionStore {
        override val session get() = flowOf(signedInId?.let { Session("token", User(it, "Ana", "ana@x.pe")) })
        override suspend fun save(session: Session) = Unit
        override suspend fun clear() = Unit
    }

    private fun listing(ownerId: String = "carlos", status: ListingStatus = ListingStatus.ACTIVE) = Listing(
        id = "l1",
        cover = "cover",
        morePhotos = emptyList(),
        title = "Bicicleta",
        category = Category.SPORTS,
        condition = Condition.GOOD,
        wants = listOf(Category.BOOKS),
        district = "Miraflores",
        status = status,
        owner = OwnerSummary(ownerId, "Carlos R.", "Miraflores"),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun model() = ListingDetailViewModel(
        route = ListingDetailRoute("l1"),
        getListingDetail = GetListingDetail(repository),
        pauseListing = PauseListing(repository),
        resumeListing = ResumeListing(repository),
        deleteListing = DeleteListing(repository),
        hideListing = HideListing(repository),
        undoHideListing = UndoHideListing(repository),
        observeSession = ObserveSession(sessions),
    )

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `starts loading and then shows someone else's listing`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing()))
        val viewModel = model()
        assertEquals(DetailPhase.Loading, viewModel.state.value.phase)
        advanceUntilIdle()
        assertEquals(DetailPhase.Loaded, viewModel.state.value.phase)
        assertEquals(listing(), viewModel.state.value.listing)
        assertEquals(false, viewModel.state.value.isOwner)
        assertEquals("detail:l1", calls.single())
    }

    @Test fun `the owner's own listing is recognised from the session`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(ownerId = "viewer")))
        val viewModel = model()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isOwner)
    }

    @Test fun `a cached listing is flagged as offline`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(), fromCache = true))
        val viewModel = model()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.offline)
    }

    @Test fun `not found becomes the unavailable state`() = runTest(dispatcher) {
        results["detail"] = Outcome.Failure(ItemsError.NotFound)
        val viewModel = model()
        advanceUntilIdle()
        assertEquals(DetailPhase.Unavailable, viewModel.state.value.phase)
    }

    @Test fun `offline failure can be retried`() = runTest(dispatcher) {
        results["detail"] = Outcome.Failure(ItemsError.Offline)
        val viewModel = model()
        advanceUntilIdle()
        assertEquals(DetailPhase.Failed(offline = true), viewModel.state.value.phase)
        results["detail"] = Outcome.Success(Loaded(listing()))
        viewModel.retry()
        assertEquals(DetailPhase.Loading, viewModel.state.value.phase)
        advanceUntilIdle()
        assertEquals(DetailPhase.Loaded, viewModel.state.value.phase)
    }

    @Test fun `pausing updates the listing and offers undo`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(ownerId = "viewer")))
        results["setStatus"] = Outcome.Success(listing(ownerId = "viewer", status = ListingStatus.PAUSED))
        val viewModel = model()
        advanceUntilIdle()
        val events = mutableListOf<DetailEvent>()
        val collector = launchCollect(viewModel, events)
        viewModel.pause()
        advanceUntilIdle()
        assertEquals(ListingStatus.PAUSED, viewModel.state.value.listing?.status)
        assertEquals(listOf<DetailEvent>(DetailEvent.Paused), events)
        collector.cancel()
    }

    @Test fun `resuming makes the listing active again`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(ownerId = "viewer", status = ListingStatus.PAUSED)))
        results["setStatus"] = Outcome.Success(listing(ownerId = "viewer"))
        val viewModel = model()
        advanceUntilIdle()
        val events = mutableListOf<DetailEvent>()
        val collector = launchCollect(viewModel, events)
        viewModel.resume()
        advanceUntilIdle()
        assertEquals(ListingStatus.ACTIVE, viewModel.state.value.listing?.status)
        assertEquals(listOf<DetailEvent>(DetailEvent.Resumed), events)
        collector.cancel()
    }

    @Test fun `a failed pause keeps the status and reports the failure`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(ownerId = "viewer")))
        results["setStatus"] = Outcome.Failure(ItemsError.Offline)
        val viewModel = model()
        advanceUntilIdle()
        val events = mutableListOf<DetailEvent>()
        val collector = launchCollect(viewModel, events)
        viewModel.pause()
        advanceUntilIdle()
        assertEquals(ListingStatus.ACTIVE, viewModel.state.value.listing?.status)
        assertEquals(listOf<DetailEvent>(DetailEvent.ActionFailed), events)
        collector.cancel()
    }

    @Test fun `someone else cannot pause or delete a listing`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing()))
        val viewModel = model()
        advanceUntilIdle()
        viewModel.pause()
        viewModel.requestDelete()
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf("detail:l1"), calls)
        assertEquals(false, viewModel.state.value.confirmingDelete)
    }

    @Test fun `hiding offers undo and undo unhides`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing()))
        results["hide"] = Outcome.Success(Unit)
        results["unhide"] = Outcome.Success(Unit)
        val viewModel = model()
        advanceUntilIdle()
        val events = mutableListOf<DetailEvent>()
        val collector = launchCollect(viewModel, events)
        viewModel.hide()
        advanceUntilIdle()
        assertEquals(listOf<DetailEvent>(DetailEvent.Hidden), events)
        viewModel.undoHide()
        advanceUntilIdle()
        assertEquals(listOf("detail:l1", "hide:l1", "unhide:l1"), calls)
        collector.cancel()
    }

    @Test fun `the owner cannot hide their own listing`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(ownerId = "viewer")))
        val viewModel = model()
        advanceUntilIdle()
        viewModel.hide()
        advanceUntilIdle()
        assertEquals(listOf("detail:l1"), calls)
    }

    @Test fun `delete asks first and only deletes on confirmation`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(ownerId = "viewer")))
        results["delete"] = Outcome.Success(Unit)
        val viewModel = model()
        advanceUntilIdle()
        val events = mutableListOf<DetailEvent>()
        val collector = launchCollect(viewModel, events)
        viewModel.requestDelete()
        assertTrue(viewModel.state.value.confirmingDelete)
        viewModel.dismissDelete()
        assertEquals(false, viewModel.state.value.confirmingDelete)
        assertEquals(listOf("detail:l1"), calls)
        viewModel.requestDelete()
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf("detail:l1", "delete:l1"), calls)
        assertEquals(listOf<DetailEvent>(DetailEvent.Deleted), events)
        assertEquals(false, viewModel.state.value.confirmingDelete)
        collector.cancel()
    }

    @Test fun `the delete dialog offers pausing instead`() = runTest(dispatcher) {
        results["detail"] = Outcome.Success(Loaded(listing(ownerId = "viewer")))
        results["setStatus"] = Outcome.Success(listing(ownerId = "viewer", status = ListingStatus.PAUSED))
        val viewModel = model()
        advanceUntilIdle()
        viewModel.requestDelete()
        viewModel.pause()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.value.confirmingDelete)
        assertEquals(ListingStatus.PAUSED, viewModel.state.value.listing?.status)
    }

    private fun kotlinx.coroutines.test.TestScope.launchCollect(
        viewModel: ListingDetailViewModel,
        into: MutableList<DetailEvent>,
    ) = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { into += it } }
}
