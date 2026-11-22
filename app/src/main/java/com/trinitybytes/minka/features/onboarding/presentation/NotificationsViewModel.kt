package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.application.FinishNotificationsStep
import com.trinitybytes.minka.features.onboarding.application.NotificationsNeedRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NotificationsEvent {
    /** The screen shows the system dialog for POST_NOTIFICATIONS (Android 13+). */
    data object RequestPermission : NotificationsEvent

    /** The setup continues with "Todo listo". */
    data object Done : NotificationsEvent
}

data class NotificationsUiState(val message: SetupMessage? = null)

/** Figma 1.20: the screen that explains the value, "luego el diálogo del sistema (Android 13+)". */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsNeedRequest: NotificationsNeedRequest,
    private val finishNotificationsStep: FinishNotificationsStep,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _events = Channel<NotificationsEvent>(Channel.BUFFERED)
    val events: Flow<NotificationsEvent> = _events.receiveAsFlow()

    private var finishing = false

    /** "Activar notificaciones": below Android 13 they are on already, so no dialog is shown. */
    fun onEnable() {
        if (notificationsNeedRequest()) {
            viewModelScope.launch { _events.send(NotificationsEvent.RequestPermission) }
        } else {
            finish()
        }
    }

    /** Granted or denied, the setup goes on. */
    fun onPermissionResult(@Suppress("UNUSED_PARAMETER") granted: Boolean) = finish()

    fun onNotNow() = finish()

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun finish() {
        if (finishing) return
        finishing = true
        _uiState.update { it.copy(message = null) }
        viewModelScope.launch {
            when (finishNotificationsStep()) {
                is Outcome.Success -> _events.send(NotificationsEvent.Done)
                is Outcome.Failure -> {
                    finishing = false
                    _uiState.update { it.copy(message = SetupMessage.Unexpected) }
                }
            }
        }
    }
}
