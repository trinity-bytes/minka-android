package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.RequestPasswordReset
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.ContactKind
import com.trinitybytes.minka.features.auth.domain.contactKindOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-off feedback shown in a snackbar. */
enum class RecoveryMessage { Offline, Unexpected }

/** The link was sent to [destination] (Figma 1.11). */
data class RecoverySent(val destination: String, val kind: ContactKind, val expiresInMinutes: Int)

/**
 * Every state of Figma 1.10–1.11: the single contact field (1.10) and, once [sent] is set, the
 * confirmation whose "Reenviar" waits for [resendSecondsLeft] to run out (1.11).
 */
data class PasswordRecoveryUiState(
    val contact: String = "",
    val contactError: Boolean = false,
    val isLoading: Boolean = false,
    val sent: RecoverySent? = null,
    val resendSecondsLeft: Long? = null,
    val message: RecoveryMessage? = null,
) {
    val fieldsEnabled: Boolean get() = !isLoading

    /** 1.10: one field that accepts an email or a phone, as registration does. */
    val canSubmit: Boolean get() = !isLoading && contactKindOf(contact) != null

    /** 1.11: "Reenviar se habilita al terminar la cuenta regresiva." */
    val canResend: Boolean get() = sent != null && !isLoading && resendSecondsLeft == null
}

@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val requestPasswordReset: RequestPasswordReset,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    private var countdown: Job? = null

    fun onContactChange(value: String) = _uiState.update { state ->
        if (!state.fieldsEnabled) return@update state
        // Once the message shows, it follows the typing so it disappears as soon as the value is valid.
        state.copy(contact = value, contactError = state.contactError && contactKindOf(value) == null)
    }

    /** Validation when the field loses focus, as in sign-in; an untouched empty field is not flagged. */
    fun onContactFocusLost() = _uiState.update {
        if (it.contact.isBlank()) it else it.copy(contactError = contactKindOf(it.contact) == null)
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        send(state.contact.trim())
    }

    fun resend() {
        val state = _uiState.value
        val sent = state.sent ?: return
        if (!state.canResend) return
        send(sent.destination)
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun send(contact: String) {
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = requestPasswordReset(contact)) {
                is Outcome.Success -> {
                    val reset = result.value
                    val kind = contactKindOf(reset.destination) ?: contactKindOf(contact) ?: ContactKind.Email
                    _uiState.update {
                        it.copy(isLoading = false, sent = RecoverySent(reset.destination, kind, reset.expiresInMinutes))
                    }
                    startCountdown(reset.resendAfterSeconds.toLong())
                }
                is Outcome.Failure -> _uiState.update {
                    it.copy(isLoading = false, message = result.error.toMessage())
                }
            }
        }
    }

    /** Same timing as the 1.08 lock: one tick per second, cleared when it reaches zero. */
    private fun startCountdown(seconds: Long) {
        countdown?.cancel()
        _uiState.update { it.copy(resendSecondsLeft = seconds.takeIf { s -> s > 0 }) }
        countdown = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1_000)
                left--
                _uiState.update { it.copy(resendSecondsLeft = left.takeIf { s -> s > 0 }) }
            }
        }
    }

    private fun AuthError.toMessage(): RecoveryMessage =
        if (this == AuthError.Offline) RecoveryMessage.Offline else RecoveryMessage.Unexpected
}
