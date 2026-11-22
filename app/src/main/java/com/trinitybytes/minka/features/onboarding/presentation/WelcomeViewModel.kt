package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.SavedStateHandle
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

private const val KEY_STEP = "welcome_step"

@HiltViewModel
class WelcomeViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WelcomeUiState(step = (savedStateHandle.get<Int>(KEY_STEP) ?: 0).coerceIn(0, WelcomeUiState.LAST_STEP)),
    )
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun next() = onStepShown((_uiState.value.step + 1).coerceAtMost(WelcomeUiState.LAST_STEP))

    /** "Omitir" goes straight to the access step: the app cannot be used without an account. */
    fun skip() = onStepShown(WelcomeUiState.LAST_STEP)

    /** The user swiped the carousel to [step]. */
    fun onStepShown(step: Int) {
        if (step in 0..WelcomeUiState.LAST_STEP) {
            savedStateHandle[KEY_STEP] = step
            _uiState.update { it.copy(step = step) }
        }
    }
}
