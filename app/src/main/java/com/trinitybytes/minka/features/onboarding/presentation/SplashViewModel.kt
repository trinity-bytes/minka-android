package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.features.auth.application.ObserveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the app goes once the splash has restored the session. */
enum class SplashDestination { Home, Welcome }

sealed interface SplashUiState {
    /** The session is being read from the device; the loading indicator shows (Figma 1.01). */
    data object Restoring : SplashUiState

    data class Ready(val destination: SplashDestination) : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(observeSession: ObserveSession) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Restoring)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = observeSession().first()
            val destination = if (session != null) SplashDestination.Home else SplashDestination.Welcome
            _uiState.value = SplashUiState.Ready(destination)
        }
    }
}
