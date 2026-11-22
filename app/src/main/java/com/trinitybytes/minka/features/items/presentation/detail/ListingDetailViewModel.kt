package com.trinitybytes.minka.features.items.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.ObserveSession
import com.trinitybytes.minka.features.items.application.DeleteListing
import com.trinitybytes.minka.features.items.application.GetListingDetail
import com.trinitybytes.minka.features.items.application.HideListing
import com.trinitybytes.minka.features.items.application.PauseListing
import com.trinitybytes.minka.features.items.application.ResumeListing
import com.trinitybytes.minka.features.items.application.UndoHideListing
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.Listing
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the detail screen is showing: the skeleton of 4.06, the listing, or the 4.05 and error pages. */
sealed interface DetailPhase {
    data object Loading : DetailPhase

    data object Loaded : DetailPhase

    /** Figma 4.05: the listing does not exist any more or is not visible to the viewer. */
    data object Unavailable : DetailPhase

    data class Failed(val offline: Boolean) : DetailPhase
}

data class DetailUiState(
    val phase: DetailPhase = DetailPhase.Loading,
    val listing: Listing? = null,
    /** True when the signed-in user published [listing]: 4.03 and 4.09 instead of 4.01. */
    val isOwner: Boolean = false,
    /** The listing came from the Room cache because the network was unavailable. */
    val offline: Boolean = false,
    /** Figma 4.08 is on screen. */
    val confirmingDelete: Boolean = false,
    /** An owner or hide action is running; the toolbar ignores taps meanwhile. */
    val acting: Boolean = false,
)

/** One-shot results the screen turns into snackbars or navigation. */
sealed interface DetailEvent {
    /** 4.09: "Publicación pausada" with "Deshacer". */
    data object Paused : DetailEvent

    data object Resumed : DetailEvent

    /** 4.07: "Ocultar" went through; the snackbar offers "Deshacer". */
    data object Hidden : DetailEvent

    /** The listing is gone; leave the screen. */
    data object Deleted : DetailEvent

    data object ActionFailed : DetailEvent
}

/** Listing detail (Figma flow 04): loads one listing and runs the owner and visitor actions on it. */
@HiltViewModel(assistedFactory = ListingDetailViewModel.Factory::class)
class ListingDetailViewModel @AssistedInject constructor(
    @Assisted private val route: ListingDetailRoute,
    private val getListingDetail: GetListingDetail,
    private val pauseListing: PauseListing,
    private val resumeListing: ResumeListing,
    private val deleteListing: DeleteListing,
    private val hideListing: HideListing,
    private val undoHideListing: UndoHideListing,
    private val observeSession: ObserveSession,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: ListingDetailRoute): ListingDetailViewModel
    }

    private val mutableState = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = mutableState.asStateFlow()

    private val mutableEvents = Channel<DetailEvent>(Channel.BUFFERED)
    val events: Flow<DetailEvent> = mutableEvents.receiveAsFlow()

    init {
        load()
    }

    fun retry() {
        mutableState.update { it.copy(phase = DetailPhase.Loading) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = getListingDetail(route.listingId)) {
                is Outcome.Success -> {
                    val listing = result.value.value
                    val viewerId = observeSession().first()?.user?.id
                    mutableState.update {
                        it.copy(
                            phase = DetailPhase.Loaded,
                            listing = listing,
                            isOwner = viewerId != null && viewerId == listing.owner.id,
                            offline = result.value.fromCache,
                        )
                    }
                }
                is Outcome.Failure -> mutableState.update {
                    it.copy(phase = failedPhase(result.error))
                }
            }
        }
    }

    private fun failedPhase(error: ItemsError): DetailPhase = when (error) {
        ItemsError.NotFound -> DetailPhase.Unavailable
        ItemsError.Offline -> DetailPhase.Failed(offline = true)
        else -> DetailPhase.Failed(offline = false)
    }

    /** Pauses the owner's listing directly, without confirmation (audit P4); also closes 4.08. */
    fun pause() = changeStatus(DetailEvent.Paused) { pauseListing(it) }

    /** 4.09 "Reanudar", and the snackbar's "Deshacer" after pausing. */
    fun resume() = changeStatus(DetailEvent.Resumed) { resumeListing(it) }

    private fun changeStatus(
        success: DetailEvent,
        action: suspend (String) -> Outcome<Listing, ItemsError>,
    ) {
        val current = mutableState.value
        if (!current.isOwner || current.acting || current.listing == null) return
        mutableState.update { it.copy(acting = true, confirmingDelete = false) }
        viewModelScope.launch {
            when (val result = action(route.listingId)) {
                is Outcome.Success -> {
                    mutableState.update { it.copy(listing = result.value, acting = false) }
                    mutableEvents.send(success)
                }
                is Outcome.Failure -> fail()
            }
        }
    }

    /** Figma 4.08 opens from the toolbar's "Borrar". */
    fun requestDelete() {
        if (mutableState.value.isOwner) mutableState.update { it.copy(confirmingDelete = true) }
    }

    fun dismissDelete() = mutableState.update { it.copy(confirmingDelete = false) }

    fun confirmDelete() {
        val current = mutableState.value
        if (!current.isOwner || !current.confirmingDelete || current.acting) return
        mutableState.update { it.copy(acting = true, confirmingDelete = false) }
        viewModelScope.launch {
            when (deleteListing(route.listingId)) {
                is Outcome.Success -> mutableEvents.send(DetailEvent.Deleted)
                is Outcome.Failure -> fail()
            }
        }
    }

    /** Figma 4.07 "Ocultar": only someone else's listing can be hidden. */
    fun hide() {
        val current = mutableState.value
        if (current.isOwner || current.acting || current.listing == null) return
        mutableState.update { it.copy(acting = true) }
        viewModelScope.launch {
            when (hideListing(route.listingId)) {
                is Outcome.Success -> {
                    mutableState.update { it.copy(acting = false) }
                    mutableEvents.send(DetailEvent.Hidden)
                }
                is Outcome.Failure -> fail()
            }
        }
    }

    /** The "Deshacer" of the hide snackbar. */
    fun undoHide() {
        viewModelScope.launch {
            if (undoHideListing(route.listingId) is Outcome.Failure) mutableEvents.send(DetailEvent.ActionFailed)
        }
    }

    private suspend fun fail() {
        mutableState.update { it.copy(acting = false) }
        mutableEvents.send(DetailEvent.ActionFailed)
    }
}
