package com.trinitybytes.minka.features.onboarding.presentation

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

/** Figma 1.19: category chips; "Continuar" waits for one, "Omitir" skips them. */
data class InterestsUiState(
    val interests: List<Interest> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: SetupMessage? = null,
) {
    /** "Elige al menos una categoría". */
    val canContinue: Boolean get() = selectedIds.isNotEmpty() && !isSaving
}

@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val getInterests: GetInterests,
    private val saveInterests: SaveInterests,
    private val skipInterests: SkipInterests,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterestsUiState())
    val uiState: StateFlow<InterestsUiState> = _uiState.asStateFlow()

    private val _events = Channel<InterestsEvent>(Channel.BUFFERED)
    val events: Flow<InterestsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val result = getInterests()) {
                is Outcome.Success -> _uiState.update { it.copy(interests = result.value, isLoading = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, message = result.error.toMessage()) }
            }
        }
    }

    fun onInterestToggled(id: String) = _uiState.update { state ->
        if (state.isSaving) return@update state
        state.copy(selectedIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id)
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

    /** 1.19 "Se puede omitir". */
    fun onSkip() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            skipInterests()
            _events.send(InterestsEvent.Done)
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }
}
