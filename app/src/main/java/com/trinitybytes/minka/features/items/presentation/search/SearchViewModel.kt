package com.trinitybytes.minka.features.items.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.AddRecentSearch
import com.trinitybytes.minka.features.items.application.ClearRecentSearches
import com.trinitybytes.minka.features.items.application.GetRecentSearches
import com.trinitybytes.minka.features.items.application.GetSearchSuggestions
import com.trinitybytes.minka.features.items.application.SearchListings
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.SearchQuery
import com.trinitybytes.minka.features.items.domain.SearchSort
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which of the Buscar screens is showing: 3.01 recents, 3.02 suggestions or 3.03-3.05 results. */
enum class SearchStage {
    Browse,
    Suggest,
    Results,
}

/** Why the last search failed; the screen offers a retry for both. */
enum class SearchError {
    Offline,
    Unexpected,
}

/** The filters that narrow a search (Figma 3.05, 3.07). Sorting is part of the sheet but is not counted. */
data class SearchFilters(
    val category: Category? = null,
    val condition: Condition? = null,
    val distanceKm: Double? = null,
    val sort: SearchSort = SearchSort.NEAREST,
) {
    /** The number in "Filtros · 3": category, minimum condition and distance. */
    val activeCount: Int get() = listOfNotNull(category, condition, distanceKm).size
}

/** A title that matches the typed text, with what the viewer needs to pick it (Figma 3.02). */
data class SearchSuggestion(
    val title: String,
    val category: Category?,
    /** Active listings near the viewer whose title contains [title]. */
    val nearbyCount: Int,
)

/** The filter sheet (Figma 3.07): the [draft] being edited and how many results it would show. */
data class FilterSheetState(
    val draft: SearchFilters,
    /** Null until the first count arrives. */
    val count: Int? = null,
)

data class SearchUiState(
    val text: String = "",
    val filters: SearchFilters = SearchFilters(),
    val stage: SearchStage = SearchStage.Browse,
    val recents: List<String> = emptyList(),
    val suggestions: List<SearchSuggestion> = emptyList(),
    val results: List<Listing> = emptyList(),
    val loading: Boolean = false,
    val error: SearchError? = null,
    val filterSheet: FilterSheetState? = null,
)

/**
 * Buscar (Figma flow 03). The text, filters and stage live in the [SavedStateHandle], so rotation and process
 * death bring the same results back; suggestions, results and the open sheet are rebuilt or dropped.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchListings: SearchListings,
    private val getSuggestions: GetSearchSuggestions,
    private val getRecentSearches: GetRecentSearches,
    private val addRecentSearch: AddRecentSearch,
    private val clearRecentSearches: ClearRecentSearches,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restore())
    val state = mutableState.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null
    private var countJob: Job? = null

    init {
        viewModelScope.launch {
            val recents = (getRecentSearches() as? Outcome.Success)?.value.orEmpty()
            change { it.copy(recents = recents) }
        }
        when (state.value.stage) {
            SearchStage.Results -> runSearch()
            SearchStage.Suggest -> loadSuggestions(state.value.text.trim())
            SearchStage.Browse -> Unit
        }
    }

    fun onTextChange(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            onClearText()
            return
        }
        change { it.copy(text = text, stage = SearchStage.Suggest) }
        loadSuggestions(trimmed)
    }

    /** The keyboard's search key: searches the text, ignoring blank input. */
    fun onSubmit() {
        val text = state.value.text.trim()
        if (text.isEmpty()) return
        change { it.copy(text = text, stage = SearchStage.Results, suggestions = emptyList()) }
        runSearch()
        viewModelScope.launch {
            val recents = (addRecentSearch(text) as? Outcome.Success)?.value ?: return@launch
            change { it.copy(recents = recents) }
        }
    }

    /** A row of "Recientes" or of the suggestions: fills the field and searches it. */
    fun onSelectRecent(text: String) {
        change { it.copy(text = text) }
        onSubmit()
    }

    /** A category disc of 3.01: results for that category alone. */
    fun onSelectCategory(category: Category) {
        change {
            it.copy(
                text = "",
                filters = it.filters.copy(category = category),
                stage = SearchStage.Results,
                suggestions = emptyList(),
            )
        }
        runSearch()
    }

    /** The "x" of the field. */
    fun onClearText() {
        suggestionsJob?.cancel()
        if (state.value.hasFilters) {
            change { it.copy(text = "", stage = SearchStage.Results, suggestions = emptyList()) }
            runSearch()
        } else {
            change {
                it.copy(text = "", stage = SearchStage.Browse, suggestions = emptyList(), results = emptyList())
            }
        }
    }

    /** The back arrow of the field: leaves the search and forgets text and filters. */
    fun onExit() {
        searchJob?.cancel()
        suggestionsJob?.cancel()
        countJob?.cancel()
        change {
            it.copy(
                text = "",
                filters = SearchFilters(),
                stage = SearchStage.Browse,
                suggestions = emptyList(),
                results = emptyList(),
                loading = false,
                error = null,
                filterSheet = null,
            )
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            if (clearRecentSearches() is Outcome.Success) change { it.copy(recents = emptyList()) }
        }
    }

    fun retry() = runSearch()

    /** "Quitar filtros" of the empty state: drops the category, condition and distance, keeps the text. */
    fun removeFilters() {
        change { it.copy(filters = it.filters.copy(category = null, condition = null, distanceKm = null)) }
        runSearch()
    }

    fun openFilters() {
        change { it.copy(filterSheet = FilterSheetState(draft = it.filters)) }
        refreshDraftCount()
    }

    fun closeFilters() {
        countJob?.cancel()
        change { it.copy(filterSheet = null) }
    }

    /** Null means no distance limit. */
    fun setDraftDistance(km: Double?) = editDraft { it.copy(distanceKm = km) }

    fun setDraftCondition(condition: Condition?) = editDraft { it.copy(condition = condition) }

    fun setDraftSort(sort: SearchSort) = editDraft { it.copy(sort = sort) }

    /** "Limpiar": distance, condition and sort back to default. The category chosen in 3.01 stays. */
    fun clearDraft() = editDraft { it.copy(condition = null, distanceKm = null, sort = SearchSort.NEAREST) }

    /** "Ver N resultados". */
    fun applyFilters() {
        val draft = state.value.filterSheet?.draft ?: return
        countJob?.cancel()
        change { it.copy(filters = draft, filterSheet = null, stage = SearchStage.Results) }
        runSearch()
    }

    private fun editDraft(transform: (SearchFilters) -> SearchFilters) {
        change { s ->
            val sheet = s.filterSheet ?: return@change s
            s.copy(filterSheet = sheet.copy(draft = transform(sheet.draft), count = null))
        }
        refreshDraftCount()
    }

    private fun runSearch() {
        searchJob?.cancel()
        val query = state.value.query()
        change { it.copy(loading = true, error = null) }
        searchJob = viewModelScope.launch {
            when (val result = searchListings(query)) {
                is Outcome.Success -> change { it.copy(loading = false, results = result.value) }
                is Outcome.Failure -> change {
                    it.copy(loading = false, results = emptyList(), error = result.error.toSearchError())
                }
            }
        }
    }

    private fun loadSuggestions(text: String) {
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            val titles = (getSuggestions(text) as? Outcome.Success)?.value.orEmpty()
            val matches = (searchListings(SearchQuery(text = text)) as? Outcome.Success)?.value.orEmpty()
            val suggestions = titles.map { title -> suggestion(title, matches) }
            change {
                val stale = it.stage != SearchStage.Suggest || it.text.trim() != text
                if (stale) it else it.copy(suggestions = suggestions)
            }
        }
    }

    private fun refreshDraftCount() {
        countJob?.cancel()
        val sheet = state.value.filterSheet ?: return
        countJob = viewModelScope.launch {
            val count = (searchListings(queryOf(state.value.text, sheet.draft)) as? Outcome.Success)?.value?.size
            change { s ->
                val current = s.filterSheet
                if (current?.draft == sheet.draft) s.copy(filterSheet = current.copy(count = count)) else s
            }
        }
    }

    private fun suggestion(title: String, matches: List<Listing>): SearchSuggestion {
        val containing = matches.filter { it.title.contains(title, ignoreCase = true) }
        val exact = matches.firstOrNull { it.title.equals(title, ignoreCase = true) }
        return SearchSuggestion(title, (exact ?: containing.firstOrNull())?.category, containing.size)
    }

    private fun change(transform: (SearchUiState) -> SearchUiState) {
        val next = transform(mutableState.value)
        mutableState.value = next
        save(next)
    }

    private fun save(state: SearchUiState) {
        savedState[TEXT_KEY] = state.text
        savedState[CATEGORY_KEY] = state.filters.category?.name
        savedState[CONDITION_KEY] = state.filters.condition?.name
        savedState[DISTANCE_KEY] = state.filters.distanceKm
        savedState[SORT_KEY] = state.filters.sort.name
        savedState[STAGE_KEY] = state.stage.name
    }

    private fun restore() = SearchUiState(
        text = savedState.get<String>(TEXT_KEY).orEmpty(),
        filters = SearchFilters(
            category = savedEnum<Category>(CATEGORY_KEY),
            condition = savedEnum<Condition>(CONDITION_KEY),
            distanceKm = savedState.get<Double>(DISTANCE_KEY),
            sort = savedEnum<SearchSort>(SORT_KEY) ?: SearchSort.NEAREST,
        ),
        stage = savedEnum<SearchStage>(STAGE_KEY) ?: SearchStage.Browse,
    )

    private inline fun <reified E : Enum<E>> savedEnum(key: String): E? =
        savedState.get<String>(key)?.let { name -> enumValues<E>().firstOrNull { it.name == name } }

    private companion object {
        const val TEXT_KEY = "search_text"
        const val CATEGORY_KEY = "search_category"
        const val CONDITION_KEY = "search_condition"
        const val DISTANCE_KEY = "search_distance"
        const val SORT_KEY = "search_sort"
        const val STAGE_KEY = "search_stage"
    }
}

private val SearchUiState.hasFilters: Boolean get() = filters.activeCount > 0

private fun SearchUiState.query() = queryOf(text, filters)

private fun queryOf(text: String, filters: SearchFilters) = SearchQuery(
    text = text.trim(),
    category = filters.category,
    condition = filters.condition,
    distanceKm = filters.distanceKm,
    sort = filters.sort,
)

private fun ItemsError.toSearchError() =
    if (this == ItemsError.Offline) SearchError.Offline else SearchError.Unexpected
