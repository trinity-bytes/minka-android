package com.trinitybytes.minka.features.profile.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.DeleteListing
import com.trinitybytes.minka.features.items.application.GetMyDrafts
import com.trinitybytes.minka.features.items.application.GetMyListings
import com.trinitybytes.minka.features.items.application.PauseListing
import com.trinitybytes.minka.features.items.application.ResumeListing
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ItemsOutcome
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.Loaded
import com.trinitybytes.minka.features.items.domain.SavedDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The status chips of Figma 11.05 (audit P1 and P3); "Reservadas" and "Todas" wait for the exchanges phase. */
enum class MyListingsFilter { ACTIVE, PAUSED, DRAFTS }

data class MyListingsUiState(
    val loading: Boolean = true,
    val filter: MyListingsFilter = MyListingsFilter.ACTIVE,
    val active: List<Listing> = emptyList(),
    val paused: List<Listing> = emptyList(),
    val drafts: List<SavedDraft> = emptyList(),
    /** Something shown comes from the device cache, or a refresh failed for lack of network. */
    val offline: Boolean = false,
    /** Nothing could be loaded: the screen shows the full error state. */
    val error: Boolean = false,
    /** The listing the expanded detail pane manages. */
    val selectedId: String? = null,
    /** The listing or draft waiting for the confirmation dialog of 4.08. */
    val pendingDelete: String? = null,
)

/** One-shot messages of 11.05 and 11.06. */
sealed interface MyListingsEvent {
    /** Pausing needs no dialog: the snackbar offers "Deshacer" for [listingId] (audit P4). */
    data class Paused(val listingId: String) : MyListingsEvent

    data object Resumed : MyListingsEvent

    data object Deleted : MyListingsEvent

    data object Failed : MyListingsEvent
}

/** "Mis publicaciones" (Figma 11.05, 11.06): the viewer's listings by status, and drafts to continue. */
@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val getMyListings: GetMyListings,
    private val getMyDrafts: GetMyDrafts,
    private val pauseListing: PauseListing,
    private val resumeListing: ResumeListing,
    private val deleteListing: DeleteListing,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MyListingsUiState(filter = savedFilter()))
    val state = mutableState.asStateFlow()

    private val mutableEvents = Channel<MyListingsEvent>(Channel.BUFFERED)
    val events: Flow<MyListingsEvent> = mutableEvents.receiveAsFlow()

    init {
        refresh()
    }

    private fun savedFilter(): MyListingsFilter {
        val name = savedState.get<String>(FILTER_KEY)
        return MyListingsFilter.entries.firstOrNull { it.name == name } ?: MyListingsFilter.ACTIVE
    }

    fun selectFilter(filter: MyListingsFilter) {
        savedState[FILTER_KEY] = filter.name
        mutableState.update { it.copy(filter = filter) }
    }

    fun select(listingId: String) = mutableState.update { it.copy(selectedId = listingId) }

    fun refresh() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = it.error, error = false) }
            load()
        }
    }

    fun pause(listingId: String) = change(listingId, MyListingsEvent.Paused(listingId)) { pauseListing(it) }

    fun resume(listingId: String) = change(listingId, MyListingsEvent.Resumed) { resumeListing(it) }

    /** The "Deshacer" of the pause snackbar: back to active without another message. */
    fun undoPause(listingId: String) = change(listingId, null) { resumeListing(it) }

    fun requestDelete(id: String) = mutableState.update { it.copy(pendingDelete = id) }

    fun cancelDelete() = mutableState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val id = mutableState.value.pendingDelete ?: return
        mutableState.update { it.copy(pendingDelete = null) }
        change(id, MyListingsEvent.Deleted) { deleteListing(it) }
    }

    private fun change(id: String, success: MyListingsEvent?, action: suspend (String) -> ItemsOutcome<*>) {
        viewModelScope.launch {
            when (action(id)) {
                is Outcome.Success -> {
                    load()
                    if (success != null) mutableEvents.send(success)
                }
                is Outcome.Failure -> mutableEvents.send(MyListingsEvent.Failed)
            }
        }
    }

    private suspend fun load() = coroutineScope {
        val active = async { getMyListings(ListingStatus.ACTIVE) }
        val paused = async { getMyListings(ListingStatus.PAUSED) }
        val drafts = async { getMyDrafts() }
        show(active.await(), paused.await(), drafts.await())
    }

    private fun show(
        active: ItemsOutcome<Loaded<List<Listing>>>,
        paused: ItemsOutcome<Loaded<List<Listing>>>,
        drafts: ItemsOutcome<List<SavedDraft>>,
    ) {
        val failures = listOf(active, paused, drafts).filterIsInstance<Outcome.Failure<ItemsError>>()
        val cached = listOf(active, paused).any { it is Outcome.Success && it.value.fromCache }
        mutableState.update { state ->
            state.copy(
                loading = false,
                active = (active as? Outcome.Success)?.value?.value ?: state.active,
                paused = (paused as? Outcome.Success)?.value?.value ?: state.paused,
                drafts = (drafts as? Outcome.Success)?.value ?: state.drafts,
                offline = cached || failures.any { it.error == ItemsError.Offline },
                error = failures.size == FILTERS_LOADED,
            )
        }
    }

    private companion object {
        const val FILTER_KEY = "my_listings_filter"
        const val FILTERS_LOADED = 3
    }
}
