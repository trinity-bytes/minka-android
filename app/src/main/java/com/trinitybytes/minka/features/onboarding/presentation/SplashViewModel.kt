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

/** Where the app goes once the splash has restored the session. */
sealed interface SplashDestination {
    data object Home : SplashDestination
    data object Welcome : SplashDestination

    /** Back to the profile setup step a signed-in user left unfinished (S4). */
    data class Setup(val step: SetupStep) : SplashDestination
}

sealed interface SplashUiState {
    /** The session is being read from the device; the loading indicator shows (Figma 1.01). */
    data object Restoring : SplashUiState

    data class Ready(val destination: SplashDestination) : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(resolveLaunch: ResolveLaunch) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Restoring)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

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
