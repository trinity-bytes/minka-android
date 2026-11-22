package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.application.CompleteOnboarding
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
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

sealed interface AllSetEvent {
    /** Onboarding is complete; Inicio opens with nothing behind it. */
    data object GoHome : AllSetEvent
}

/** Figma 1.21: "¡Todo listo, Ana!" and the district chosen in 1.16, when the profile has it. */
data class AllSetUiState(
    val firstName: String = "",
    val districtName: String? = null,
    val isFinishing: Boolean = false,
    val message: SetupMessage? = null,
)

@HiltViewModel
class AllSetViewModel @Inject constructor(
    private val getSetupSummary: GetSetupSummary,
    private val completeOnboarding: CompleteOnboarding,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllSetUiState())
    val uiState: StateFlow<AllSetUiState> = _uiState.asStateFlow()

    private val _events = Channel<AllSetEvent>(Channel.BUFFERED)
    val events: Flow<AllSetEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val summary = getSetupSummary()
            _uiState.update { it.copy(firstName = summary.firstName, districtName = summary.districtName) }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** "Explorar Mink'a": 1.21 "lleva a Inicio". */
    fun onExplore() {
        if (_uiState.value.isFinishing) return
        _uiState.update { it.copy(isFinishing = true, message = null) }
        viewModelScope.launch {
            when (completeOnboarding()) {
                is Outcome.Success -> _events.send(AllSetEvent.GoHome)
                is Outcome.Failure -> {
                    _uiState.update { it.copy(isFinishing = false, message = SetupMessage.Unexpected) }
                }
            }
        }
    }
}
