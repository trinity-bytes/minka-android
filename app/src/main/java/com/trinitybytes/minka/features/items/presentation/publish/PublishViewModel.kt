package com.trinitybytes.minka.features.items.presentation.publish

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.PublishListing
import com.trinitybytes.minka.features.items.application.ResumeListingDraft
import com.trinitybytes.minka.features.items.application.SaveListingDraft
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.DraftError
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.MAX_PHOTOS
import com.trinitybytes.minka.features.items.domain.validateBasics
import com.trinitybytes.minka.features.items.domain.validateExchange
import com.trinitybytes.minka.features.items.domain.validateLocation
import com.trinitybytes.minka.features.items.domain.validateReview
import com.trinitybytes.minka.features.onboarding.application.GetDistricts
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
import com.trinitybytes.minka.navigation.PublishRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The Publicar flow (Figma 5.01–5.11): a draft that grows page by page until it is published. */
@HiltViewModel(assistedFactory = PublishViewModel.Factory::class)
class PublishViewModel @AssistedInject constructor(
    private val saveDraft: SaveListingDraft,
    private val publishListing: PublishListing,
    private val resumeDraft: ResumeListingDraft,
    private val getDistricts: GetDistricts,
    private val getSetupSummary: GetSetupSummary,
    private val savedStateHandle: SavedStateHandle,
    @Assisted private val route: PublishRoute? = null,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(route: PublishRoute): PublishViewModel
    }

    private val restored = savedStateHandle.restoreForm()

    private val _uiState = MutableStateFlow(
        restored?.let { PublishUiState(draft = it.draft, page = it.page, returnToReview = it.returnToReview) }
            ?: PublishUiState(
                phase = if (route?.draftId != null) PublishPhase.Loading else PublishPhase.Editing,
            ),
    )
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    private val _events = Channel<PublishEvent>(Channel.BUFFERED)
    val events: Flow<PublishEvent> = _events.receiveAsFlow()

    /** The server copy of the draft, or the blank form: what "unsaved changes" compares against. */
    private var baseline: ListingDraft = restored?.baseline ?: ListingDraft()
    private var draftId: String? = restored?.draftId ?: route?.draftId

    init {
        when {
            restored != null -> Unit
            route?.draftId != null -> load(route.draftId)
            else -> prefillDistrict()
        }
    }

    // Step 1

    fun onAddPhotosTapped() {
        if (_uiState.value.canAddPhotos) show(PublishOverlay.PhotoSource)
    }

    /** Adds what the camera or the picker returned, up to the maximum; extra photos are dropped. */
    fun onPhotosPicked(uris: List<String>) = change { state ->
        val room = MAX_PHOTOS - state.draft.photos.size
        val added = uris.filter { it !in state.draft.photos }.distinct().take(room.coerceAtLeast(0))
        state.edit(overlay = null) { it.copy(photos = it.photos + added) }
    }

    fun onPhotoTapped(index: Int) = change { state ->
        if (index in state.draft.photos.indices) state.copy(overlay = PublishOverlay.PhotoActions(index)) else state
    }

    fun onMakeCover(index: Int) = change { state ->
        val photos = state.draft.photos
        if (index !in photos.indices) return@change state.copy(overlay = null)
        val reordered = listOf(photos[index]) + photos.filterIndexed { i, _ -> i != index }
        state.edit(overlay = null) { it.copy(photos = reordered) }
    }

    fun onPhotoRemoved(index: Int) = change { state ->
        val photos = state.draft.photos
        if (index !in photos.indices) return@change state.copy(overlay = null)
        state.edit(overlay = null) { it.copy(photos = photos.filterIndexed { i, _ -> i != index }) }
    }

    fun onTitleChange(value: String) = change { state -> state.edit { it.copy(title = value) } }

    fun onCategoryRowTapped() = show(PublishOverlay.Category)

    fun onCategoryChosen(category: Category) = change { state ->
        state.edit(overlay = null) { it.copy(category = category) }
    }

    fun onConditionChosen(condition: Condition) = change { state -> state.edit { it.copy(condition = condition) } }

    fun onDetailsTapped() = show(PublishOverlay.Details)

    /** 5.04 "Listo": [estimatedSoles] is what the field holds, in soles; blank or invalid clears the value. */
    fun onDetailsDone(description: String, estimatedSoles: String) = change { state ->
        state.edit(overlay = null) {
            it.copy(description = description.trim().ifEmpty { null }, estimatedValue = centavosOf(estimatedSoles))
        }
    }

    // Step 2

    /** Choosing "Lo que sea" replaces the other categories; choosing another one removes "Lo que sea". */
    fun onWantToggled(category: Category) = change { state ->
        state.edit { draft ->
            val wants = when {
                category in draft.wants -> draft.wants - category
                category == Category.ANY -> listOf(Category.ANY)
                else -> (draft.wants - Category.ANY) + category
            }
            draft.copy(wants = wants)
        }
    }

    fun onDistrictRowTapped() {
        show(PublishOverlay.District)
        if (_uiState.value.districts != null) return
        viewModelScope.launch {
            val result = getDistricts()
            if (result is Outcome.Success) change { it.copy(districts = result.value) }
        }
    }

    fun onDistrictChosen(name: String) = change { state -> state.edit(overlay = null) { it.copy(district = name) } }

    fun onMeetingPointRowTapped() = show(PublishOverlay.MeetingPoint)

    fun onMeetingPointDone(text: String) = change { state ->
        state.edit(overlay = null) { it.copy(meetingPoint = text.trim().ifEmpty { null }) }
    }

    fun onHideLocationChange(hide: Boolean) = change { state -> state.edit { it.copy(hideExactLocation = hide) } }

    fun onHomeDeliveryChange(delivery: Boolean) = change { state -> state.edit { it.copy(homeDelivery = delivery) } }

    // Navigation between pages

    /** 5.01: "Continuar" is always enabled; it validates on tap and shows the missing fields (5.05). */
    fun onContinue() {
        val state = _uiState.value
        if (state.phase != PublishPhase.Editing) return
        if (state.page == PublishPage.Review) return onPublish()
        val errors = state.draft.validate(state.page)
        if (errors.isNotEmpty()) return change { it.copy(errors = errors) }
        change {
            val next = if (it.returnToReview) PublishPage.Review else PublishPage.entries[it.page.ordinal + 1]
            it.copy(page = next, errors = emptySet(), returnToReview = false)
        }
    }

    /** The top bar arrow and the system back: one page back, or the exit confirmation from the first. */
    fun onBack() {
        val state = _uiState.value
        if (state.overlay != null) return dismissOverlay()
        if (state.phase != PublishPhase.Editing) return
        if (state.returnToReview) {
            return change { it.copy(page = PublishPage.Review, returnToReview = false, errors = emptySet()) }
        }
        if (state.page == PublishPage.Basics) return onClose()
        change { it.copy(page = PublishPage.entries[it.page.ordinal - 1], errors = emptySet()) }
    }

    /** The close icon: leaves, or offers to keep the draft when there is unsaved work (5.11). */
    fun onClose() {
        if (_uiState.value.draft == baseline) {
            _events.trySend(PublishEvent.Close)
        } else {
            show(PublishOverlay.ExitConfirm)
        }
    }

    /** "Editar" on the review: edits [page] and comes back to the review afterwards. */
    fun onEdit(page: PublishPage) = change {
        it.copy(
            page = page,
            errors = emptySet(),
            overlay = null,
            returnToReview = it.page == PublishPage.Review && page != it.page,
        )
    }

    fun dismissOverlay() = change { it.copy(overlay = null) }

    // Saving and publishing

    /** "Guardar borrador": keeps the draft on the account and the person on the form. */
    fun onSaveDraft() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch { save(close = false) }
    }

    fun onExitSave() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch { save(close = true) }
    }

    fun onExitDiscard() {
        _events.trySend(PublishEvent.Close)
    }

    fun onPublish() {
        val state = _uiState.value
        if (state.phase != PublishPhase.Editing) return
        if (state.draft.validateReview().isNotEmpty()) {
            val page = listOf(PublishPage.Basics, PublishPage.Wants, PublishPage.Location)
                .first { state.draft.validate(it).isNotEmpty() }
            return change {
                it.copy(page = page, errors = state.draft.validate(page), overlay = null, returnToReview = false)
            }
        }
        change { it.copy(phase = PublishPhase.Publishing, message = null, overlay = null) }
        viewModelScope.launch { publish() }
    }

    fun onPublishAnother() {
        baseline = ListingDraft()
        draftId = null
        savedStateHandle.clearForm()
        _uiState.value = PublishUiState()
        persist()
        prefillDistrict()
    }

    fun onRetryLoad() {
        val id = route?.draftId ?: return
        change { it.copy(phase = PublishPhase.Loading) }
        load(id)
    }

    fun onMessageShown() = change { it.copy(message = null) }

    private suspend fun save(close: Boolean) {
        val draft = _uiState.value.draft
        change { it.copy(isSaving = true, message = null) }
        when (val result = saveDraft(draftId, draft)) {
            is Outcome.Success -> {
                draftId = result.value.id
                baseline = draft
                if (close) {
                    _events.send(PublishEvent.Close)
                    change { it.copy(isSaving = false) }
                } else {
                    change { it.copy(isSaving = false, message = PublishMessage.DraftSaved) }
                }
            }
            is Outcome.Failure -> change {
                it.copy(isSaving = false, overlay = null, message = result.error.toMessage())
            }
        }
    }

    /** Saves the draft when it changed, then publishes it; a failure leaves the draft on the account. */
    private suspend fun publish() {
        val draft = _uiState.value.draft
        if (draftId == null || draft != baseline) {
            when (val saved = saveDraft(draftId, draft)) {
                is Outcome.Success -> {
                    draftId = saved.value.id
                    baseline = draft
                }
                is Outcome.Failure -> return backToReview(saved.error.toMessage())
            }
        }
        when (val result = publishListing(checkNotNull(draftId))) {
            is Outcome.Success -> {
                draftId = null
                baseline = draft
                savedStateHandle.clearForm()
                _uiState.value = _uiState.value.copy(phase = PublishPhase.Published, published = result.value)
            }
            is Outcome.Failure -> backToReview(PublishMessage.PublishFailed)
        }
    }

    private fun backToReview(message: PublishMessage) = change {
        it.copy(phase = PublishPhase.Editing, page = PublishPage.Review, message = message)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            when (val result = resumeDraft(id)) {
                is Outcome.Success -> {
                    baseline = result.value.draft
                    draftId = result.value.id
                    change { it.copy(phase = PublishPhase.Editing, draft = result.value.draft) }
                }
                is Outcome.Failure -> change { it.copy(phase = PublishPhase.LoadFailed) }
            }
        }
    }

    /** A new listing starts in the district of the profile; the prefill is not unsaved work. */
    private fun prefillDistrict() {
        viewModelScope.launch {
            val name = getSetupSummary().districtName ?: return@launch
            if (_uiState.value.draft.district.isNotBlank() || baseline.district.isNotBlank()) return@launch
            baseline = baseline.copy(district = name)
            change { it.edit { draft -> draft.copy(district = name) } }
        }
    }

    private fun show(overlay: PublishOverlay) = change { it.copy(overlay = overlay) }

    private fun change(transform: (PublishUiState) -> PublishUiState) {
        _uiState.value = transform(_uiState.value)
        persist()
    }

    private fun persist() {
        val state = _uiState.value
        if (state.phase == PublishPhase.Published) return
        savedStateHandle.saveForm(state.draft, baseline, state.page, draftId, state.returnToReview)
    }
}

/** Applies [transform] to the draft; a shown error list follows the edit so fixed fields lose their message. */
private fun PublishUiState.edit(
    overlay: PublishOverlay? = this.overlay,
    transform: (ListingDraft) -> ListingDraft,
): PublishUiState {
    val updated = transform(draft)
    return copy(
        draft = updated,
        overlay = overlay,
        errors = if (errors.isEmpty()) errors else updated.validate(page),
    )
}

private fun ListingDraft.validate(page: PublishPage): Set<DraftError> = when (page) {
    PublishPage.Basics -> validateBasics()
    PublishPage.Wants -> validateExchange()
    PublishPage.Location -> validateLocation()
    PublishPage.Review -> validateReview()
}

private fun ItemsError.toMessage(): PublishMessage = when (this) {
    ItemsError.Offline -> PublishMessage.Offline
    else -> PublishMessage.Unexpected
}

/** Soles typed by the person ("120", "45,50") as centavos; null when blank, invalid or not positive. */
internal fun centavosOf(soles: String): Long? {
    val amount = soles.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (amount.signum() <= 0) return null
    return amount.multiply(BigDecimal(100)).toLong()
}
