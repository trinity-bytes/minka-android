package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** The welcome carousel (Figma 1.02–1.04); the last step shows the access group. */
data class WelcomeUiState(val step: Int = 0) {
    val isLastStep: Boolean get() = step == LAST_STEP

    companion object {
        const val STEP_COUNT = 3
        const val LAST_STEP = STEP_COUNT - 1
    }
}

@HiltViewModel
class WelcomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun next() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(WelcomeUiState.LAST_STEP)) }

    /** "Omitir" goes straight to the access step: the app cannot be used without an account. */
    fun skip() = _uiState.update { it.copy(step = WelcomeUiState.LAST_STEP) }

    /** The user swiped the carousel to [step]. */
    fun onStepShown(step: Int) {
        if (step in 0..WelcomeUiState.LAST_STEP) _uiState.update { it.copy(step = step) }
    }
}
