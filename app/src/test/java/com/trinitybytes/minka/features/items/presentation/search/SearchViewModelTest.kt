package com.trinitybytes.minka.features.items.presentation.search

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.AddRecentSearch
import com.trinitybytes.minka.features.items.application.ClearRecentSearches
import com.trinitybytes.minka.features.items.application.GetRecentSearches
import com.trinitybytes.minka.features.items.application.GetSearchSuggestions
import com.trinitybytes.minka.features.items.application.SearchListings
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ItemsRepository
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import com.trinitybytes.minka.features.items.domain.SearchQuery
import com.trinitybytes.minka.features.items.domain.SearchSort
import java.lang.reflect.Proxy
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

/** The real use cases over a scripted [ItemsRepository]; only the search-related calls are answered. */
private class ScriptedItems {
    var listings: List<Listing> = emptyList()
    var failure: ItemsError? = null
    var titles: List<String> = emptyList()
    var recents: List<String> = emptyList()
    val queries = mutableListOf<SearchQuery>()
    var cleared = 0

    /** Mirrors the mock server: text and condition and category narrow, distance and sort are ignored here. */
    private fun matches(query: SearchQuery) = listings.filter { listing ->
        (query.text.isBlank() || listing.title.contains(query.text, ignoreCase = true)) &&
            (query.category == null || listing.category == query.category) &&
            (query.condition == null || listing.condition.ordinal <= query.condition.ordinal) &&
            (query.distanceKm == null || (listing.distanceKm ?: 0.0) <= query.distanceKm)
    }

    val repository = Proxy.newProxyInstance(
        ItemsRepository::class.java.classLoader,
        arrayOf(ItemsRepository::class.java),
    ) { _, method, args ->
        when (method.name) {
            "search" -> {
                val query = args[0] as SearchQuery
                queries += query
                failure?.let { Outcome.Failure(it) } ?: Outcome.Success(matches(query))
            }
            "suggestions" -> Outcome.Success(titles.filter { it.contains(args[0] as String, ignoreCase = true) })
            "recentSearches" -> Outcome.Success(recents)
            "addRecentSearch" -> {
                recents = (listOf(args[0] as String) + recents.filterNot { it == args[0] }).take(10)
                Outcome.Success(recents)
            }
            "clearRecentSearches" -> {
                cleared++
                recents = emptyList()
                Outcome.Success(Unit)
            }
            else -> error("Unexpected call ${method.name}")
        }
    } as ItemsRepository
}

private fun listing(
    id: String,
    title: String,
    category: Category = Category.SPORTS,
    condition: Condition = Condition.GOOD,
    distanceKm: Double = 1.0,
) = Listing(
    id = id,
    cover = "cover",
    morePhotos = emptyList(),
    title = title,
    category = category,
    condition = condition,
    wants = listOf(Category.BOOKS),
    district = "Miraflores",
    status = ListingStatus.ACTIVE,
    owner = OwnerSummary(id = "owner", name = "Carlos", district = "Barranco"),
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
    distanceKm = distanceKm,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val items = ScriptedItems()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun model(handle: SavedStateHandle = SavedStateHandle()) = SearchViewModel(
        searchListings = SearchListings(items.repository),
        getSuggestions = GetSearchSuggestions(items.repository),
        getRecentSearches = GetRecentSearches(items.repository),
        addRecentSearch = AddRecentSearch(items.repository),
        clearRecentSearches = ClearRecentSearches(items.repository),
        savedState = handle,
    )

    @Test fun `starts on the recent searches without running a search`() = runTest(dispatcher) {
        items.recents = listOf("bicicleta", "taladro")
        val viewModel = model()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(SearchStage.Browse, state.stage)
        assertEquals(listOf("bicicleta", "taladro"), state.recents)
        assertTrue(items.queries.isEmpty())
    }

    @Test fun `typing shows suggestions with their category and how many are near`() = runTest(dispatcher) {
        items.titles = listOf("Bicicleta de montaña", "Bicicleta urbana")
        items.listings = listOf(
            listing("1", "Bicicleta de montaña"),
            listing("2", "Bicicleta urbana"),
            listing("3", "Casco de bici", Category.SPORTS),
        )
        val viewModel = model()
        viewModel.onTextChange("bici")
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(SearchStage.Suggest, state.stage)
        assertEquals(
            listOf(
                SearchSuggestion("Bicicleta de montaña", Category.SPORTS, nearbyCount = 1),
                SearchSuggestion("Bicicleta urbana", Category.SPORTS, nearbyCount = 1),
            ),
            state.suggestions,
        )
    }

    @Test fun `emptying the field goes back to the recents`() = runTest(dispatcher) {
        val viewModel = model()
        viewModel.onTextChange("bici")
        advanceUntilIdle()
        viewModel.onTextChange("")
        advanceUntilIdle()
        assertEquals(SearchStage.Browse, viewModel.state.value.stage)
        assertTrue(viewModel.state.value.suggestions.isEmpty())
    }

    @Test fun `submitting searches the trimmed text and remembers it`() = runTest(dispatcher) {
        items.listings = listOf(listing("1", "Bicicleta urbana"), listing("2", "Taladro"))
        val viewModel = model()
        viewModel.onTextChange("  bici ")
        viewModel.onSubmit()
        assertTrue(viewModel.state.value.loading)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(SearchStage.Results, state.stage)
        assertFalse(state.loading)
        assertEquals(listOf("1"), state.results.map { it.id })
        assertEquals("bici", items.queries.last().text)
        assertEquals(listOf("bici"), state.recents)
    }

    @Test fun `a blank submit does nothing`() = runTest(dispatcher) {
        val viewModel = model()
        viewModel.onTextChange("   ")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(SearchStage.Browse, viewModel.state.value.stage)
        assertTrue(items.queries.isEmpty())
    }

    @Test fun `picking a recent search or a suggestion fills the field and searches it`() = runTest(dispatcher) {
        items.listings = listOf(listing("1", "Taladro"))
        val viewModel = model()
        viewModel.onSelectRecent("taladro")
        advanceUntilIdle()
        assertEquals("taladro", viewModel.state.value.text)
        assertEquals(SearchStage.Results, viewModel.state.value.stage)
        assertEquals(listOf("1"), viewModel.state.value.results.map { it.id })
    }

    @Test fun `choosing a category searches it with no text and shows it as an applied filter`() =
        runTest(dispatcher) {
            items.listings = listOf(listing("1", "Taladro", Category.TOOLS), listing("2", "Silla", Category.HOME))
            val viewModel = model()
            viewModel.onSelectCategory(Category.HOME)
            advanceUntilIdle()
            val state = viewModel.state.value
            assertEquals(SearchStage.Results, state.stage)
            assertEquals(Category.HOME, state.filters.category)
            assertEquals("", state.text)
            assertEquals(listOf("2"), state.results.map { it.id })
            assertEquals(1, state.filters.activeCount)
        }

    @Test fun `the sheet counts what each draft would show and applies it only on confirm`() = runTest(dispatcher) {
        items.listings = listOf(
            listing("1", "Bicicleta A", condition = Condition.NEW, distanceKm = 0.8),
            listing("2", "Bicicleta B", condition = Condition.USED, distanceKm = 4.0),
            listing("3", "Bicicleta C", condition = Condition.GOOD, distanceKm = 2.0),
        )
        val viewModel = model()
        viewModel.onTextChange("bicicleta")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(3, viewModel.state.value.results.size)

        viewModel.openFilters()
        advanceUntilIdle()
        assertEquals(3, viewModel.state.value.filterSheet?.count)
        viewModel.setDraftCondition(Condition.GOOD)
        viewModel.setDraftDistance(3.0)
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.filterSheet?.count)
        assertEquals(3, viewModel.state.value.results.size)

        viewModel.applyFilters()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertNull(state.filterSheet)
        assertEquals(listOf("1", "3"), state.results.map { it.id })
        assertEquals(Condition.GOOD, items.queries.last().condition)
        assertEquals(3.0, items.queries.last().distanceKm!!, 0.0)
        assertEquals(2, state.filters.activeCount)
    }

    @Test fun `closing the sheet discards the draft`() = runTest(dispatcher) {
        val viewModel = model()
        viewModel.onTextChange("bici")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.openFilters()
        viewModel.setDraftCondition(Condition.NEW)
        viewModel.closeFilters()
        advanceUntilIdle()
        assertNull(viewModel.state.value.filterSheet)
        assertNull(viewModel.state.value.filters.condition)
    }

    @Test fun `Limpiar resets the draft but keeps the chosen category`() = runTest(dispatcher) {
        val viewModel = model()
        viewModel.onSelectCategory(Category.HOME)
        advanceUntilIdle()
        viewModel.openFilters()
        viewModel.setDraftCondition(Condition.NEW)
        viewModel.setDraftDistance(1.0)
        viewModel.setDraftSort(SearchSort.NEWEST)
        viewModel.clearDraft()
        val draft = viewModel.state.value.filterSheet!!.draft
        assertEquals(Category.HOME, draft.category)
        assertNull(draft.condition)
        assertNull(draft.distanceKm)
        assertEquals(SearchSort.NEAREST, draft.sort)
    }

    @Test fun `only the phase 3 sorts exist`() {
        assertEquals(listOf(SearchSort.NEAREST, SearchSort.NEWEST), SearchSort.entries)
    }

    @Test fun `Quitar filtros drops every filter and keeps the text`() = runTest(dispatcher) {
        items.listings = listOf(listing("1", "Patines", condition = Condition.USED, distanceKm = 5.0))
        val viewModel = model()
        viewModel.onSelectCategory(Category.SPORTS)
        viewModel.onTextChange("patines")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.openFilters()
        viewModel.setDraftCondition(Condition.NEW)
        viewModel.setDraftDistance(1.0)
        viewModel.applyFilters()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.results.isEmpty())

        viewModel.removeFilters()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(0, state.filters.activeCount)
        assertEquals("patines", state.text)
        assertEquals(listOf("1"), state.results.map { it.id })
    }

    @Test fun `the back arrow leaves the search and forgets text and filters`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val viewModel = model(handle)
        viewModel.onSelectCategory(Category.HOME)
        viewModel.onTextChange("silla")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onExit()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(SearchStage.Browse, state.stage)
        assertEquals("", state.text)
        assertNull(state.filters.category)
        assertTrue(state.results.isEmpty())
        assertEquals(SearchStage.Browse, model(handle).state.value.stage)
    }

    @Test fun `clearing the text keeps the results when a filter is still applied`() = runTest(dispatcher) {
        items.listings = listOf(listing("1", "Silla", Category.HOME))
        val viewModel = model()
        viewModel.onSelectCategory(Category.HOME)
        viewModel.onTextChange("silla")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onClearText()
        advanceUntilIdle()
        assertEquals(SearchStage.Results, viewModel.state.value.stage)
        assertEquals("", items.queries.last().text)
        assertEquals(Category.HOME, items.queries.last().category)
    }

    @Test fun `clearing the text with no filter goes back to the recents`() = runTest(dispatcher) {
        val viewModel = model()
        viewModel.onTextChange("silla")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onClearText()
        advanceUntilIdle()
        assertEquals(SearchStage.Browse, viewModel.state.value.stage)
    }

    @Test fun `query filters and stage survive process death`() = runTest(dispatcher) {
        items.listings = listOf(listing("1", "Bicicleta", condition = Condition.NEW, distanceKm = 1.0))
        val handle = SavedStateHandle()
        val first = model(handle)
        first.onSelectCategory(Category.SPORTS)
        first.onTextChange("bicicleta")
        first.onSubmit()
        advanceUntilIdle()
        first.openFilters()
        first.setDraftCondition(Condition.GOOD)
        first.setDraftDistance(3.0)
        first.setDraftSort(SearchSort.NEWEST)
        first.applyFilters()
        advanceUntilIdle()

        items.queries.clear()
        val restored = model(handle)
        advanceUntilIdle()
        val state = restored.state.value
        assertEquals(SearchStage.Results, state.stage)
        assertEquals("bicicleta", state.text)
        assertEquals(
            SearchQuery("bicicleta", Category.SPORTS, Condition.GOOD, 3.0, SearchSort.NEWEST),
            items.queries.last(),
        )
        assertEquals(listOf("1"), state.results.map { it.id })
    }

    @Test fun `Borrar empties the recent searches`() = runTest(dispatcher) {
        items.recents = listOf("bicicleta")
        val viewModel = model()
        advanceUntilIdle()
        viewModel.clearRecents()
        advanceUntilIdle()
        assertEquals(1, items.cleared)
        assertTrue(viewModel.state.value.recents.isEmpty())
    }

    @Test fun `a failed search can be retried`() = runTest(dispatcher) {
        items.failure = ItemsError.Offline
        items.listings = listOf(listing("1", "Bicicleta"))
        val viewModel = model()
        viewModel.onTextChange("bici")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(SearchError.Offline, viewModel.state.value.error)
        assertTrue(viewModel.state.value.results.isEmpty())

        items.failure = null
        viewModel.retry()
        advanceUntilIdle()
        assertNull(viewModel.state.value.error)
        assertEquals(listOf("1"), viewModel.state.value.results.map { it.id })
    }
}
