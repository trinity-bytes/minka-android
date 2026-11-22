package com.trinitybytes.minka.features.items.presentation.home

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
import com.trinitybytes.minka.features.onboarding.domain.ProfileRepository
import kotlinx.coroutines.flow.flowOf
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.GetFeed
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ItemsRepository
import com.trinitybytes.minka.features.items.domain.Loaded
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var result: Any = Outcome.Success(Loaded(Feed(emptyList(), emptyList())))
    private val repository = Proxy.newProxyInstance(
        ItemsRepository::class.java.classLoader,
        arrayOf(ItemsRepository::class.java),
    ) { _, method, _ ->
        check(method.name == "feed")
        result
    } as ItemsRepository

    private val sessions = object : SessionStore {
        override val session = flowOf<Session?>(null)
        override suspend fun save(session: Session) = Unit
        override suspend fun clear() = Unit
    }
    private val profiles = Proxy.newProxyInstance(
        ProfileRepository::class.java.classLoader,
        arrayOf(ProfileRepository::class.java),
    ) { _, _, _ -> error("No profile expected without a session") } as ProfileRepository
    private val summary = GetSetupSummary(sessions, profiles)
    private fun model(handle: SavedStateHandle = SavedStateHandle()) =
        HomeViewModel(GetFeed(repository), handle, summary)

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `initial loading becomes feed`() = runTest(dispatcher) {
        val viewModel = model()
        assertEquals(HomeUiState(), viewModel.state.value)
        advanceUntilIdle()
        assertEquals(HomeUiState(loading = false, feed = Feed(emptyList(), emptyList())), viewModel.state.value)
    }

    @Test fun `cached feed remains visible offline`() = runTest(dispatcher) {
        result = Outcome.Success(Loaded(Feed(emptyList(), emptyList()), fromCache = true))
        val viewModel = model()
        advanceUntilIdle()
        assertEquals(true, viewModel.state.value.offline)
        assertEquals(Feed(emptyList(), emptyList()), viewModel.state.value.feed)
    }

    @Test fun `failure can be retried`() = runTest(dispatcher) {
        result = Outcome.Failure(ItemsError.Unexpected)
        val viewModel = model()
        advanceUntilIdle()
        assertEquals(true, viewModel.state.value.error)
        result = Outcome.Success(Loaded(Feed(emptyList(), emptyList())))
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.value.error)
        assertEquals(Feed(emptyList(), emptyList()), viewModel.state.value.feed)
    }
    @Test fun `selected category survives refresh and saved state restoration`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val viewModel = model(handle)
        viewModel.selectCategory(Category.BOOKS)
        advanceUntilIdle()
        assertEquals(Category.BOOKS, viewModel.state.value.selectedCategory)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(Category.BOOKS, viewModel.state.value.selectedCategory)
        val restored = model(handle)
        advanceUntilIdle()
        assertEquals(Category.BOOKS, restored.state.value.selectedCategory)
        restored.selectCategory(Category.BOOKS)
        assertEquals(null, restored.state.value.selectedCategory)
    }

}
