package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.features.onboarding.application.LaunchTarget
import com.trinitybytes.minka.features.onboarding.application.ResolveLaunch
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the app starts once the system splash has restored the session. */
sealed interface SplashDestination {
    data object Home : SplashDestination
    data object Welcome : SplashDestination

    /** Back to the profile setup step a signed-in user left unfinished (S4). */
    data class Setup(val step: SetupStep) : SplashDestination
}

sealed interface SplashUiState {
    /** The session is being read from the device; the system splash stays on screen (Figma 1.01). */
    data object Restoring : SplashUiState

    data class Ready(val destination: SplashDestination) : SplashUiState
}

/**
 * Resolves where the app starts (Figma 1.01, design A1). `MainActivity` keeps the system splash on
 * screen while [keepSplashOnScreen] holds and composes the navigation graph only once the
 * destination is [SplashUiState.Ready], because a start destination cannot change afterwards.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(resolveLaunch: ResolveLaunch) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Restoring)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    /** Read by the system splash's keep-on-screen condition on every frame until it turns false. */
    val keepSplashOnScreen: Boolean
        get() = _uiState.value == SplashUiState.Restoring

    init {
        viewModelScope.launch {
            val destination = when (val target = resolveLaunch()) {
                LaunchTarget.Welcome -> SplashDestination.Welcome
                LaunchTarget.Home -> SplashDestination.Home
                is LaunchTarget.Setup -> SplashDestination.Setup(target.step)
            }
            _uiState.value = SplashUiState.Ready(destination)
        }
    }
}
