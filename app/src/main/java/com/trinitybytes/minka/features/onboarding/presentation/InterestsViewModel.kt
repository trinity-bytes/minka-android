package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.application.GetInterests
import com.trinitybytes.minka.features.onboarding.application.SaveInterests
import com.trinitybytes.minka.features.onboarding.application.SkipInterests
import com.trinitybytes.minka.features.onboarding.domain.Interest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InterestsEvent {
    /** Saved or skipped; the setup continues with notifications. */
    data object Done : InterestsEvent
}

/**
 * Figma 1.19: category chips; "Continuar" waits for one, "Omitir" skips them. A failed save shows a
 * snackbar that keeps the choice (1.22); a list that could not load shows [loadError] full screen.
 */
data class InterestsUiState(
    val interests: List<Interest> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: SetupMessage? = null,
    val loadError: LoadError? = null,
) {
    /** "Elige al menos una categoría". */
    val canContinue: Boolean get() = selectedIds.isNotEmpty() && !isSaving
}

@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val getInterests: GetInterests,
    private val saveInterests: SaveInterests,
    private val skipInterests: SkipInterests,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterestsUiState(selectedIds = savedStateHandle.get<ArrayList<String>>("interest_ids")?.toSet() ?: emptySet()))
    val uiState: StateFlow<InterestsUiState> = _uiState.asStateFlow()

    private val _events = Channel<InterestsEvent>(Channel.BUFFERED)
    val events: Flow<InterestsEvent> = _events.receiveAsFlow()

    init {
        loadInterests()
    }

    private fun loadInterests() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = getInterests()
            _uiState.update {
                when (result) {
                    is Outcome.Success -> it.copy(isLoading = false, loadError = null, interests = result.value)
                    is Outcome.Failure -> it.copy(isLoading = false, loadError = result.error.toLoadError())
                }
            }
        }
    }

    fun onInterestToggled(id: String) = _uiState.update { state ->
        if (state.isSaving) return@update state
        val selected = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
        savedStateHandle["interest_ids"] = ArrayList(selected)
        state.copy(selectedIds = selected)
    }

    fun onContinue() {
        val state = _uiState.value
        if (!state.canContinue) return
        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            // Saved in the catalog order, whatever order they were tapped in.
            val ids = state.interests.map { it.id }.filter { it in state.selectedIds }
            val result = saveInterests(ids)
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is Outcome.Success -> _events.send(InterestsEvent.Done)
                is Outcome.Failure -> _uiState.update { it.copy(message = result.error.toMessage()) }
            }
        }
    }

    /** 1.19 "Se puede omitir". Busy like a save, so a second tap or "Continuar" waits for it. */
    fun onSkip() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            val result = skipInterests()
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is Outcome.Success -> _events.send(InterestsEvent.Done)
                is Outcome.Failure -> _uiState.update { it.copy(message = result.error.toMessage()) }
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** "Reintentar": loads the list again if it failed, else saves the interests chosen now. */
    fun retry() {
        val state = _uiState.value
        when {
            state.isLoading || state.isSaving -> Unit
            state.loadError != null -> loadInterests()
            else -> onContinue()
        }
    }
}
