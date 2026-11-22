package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.core.domain.countdown
import com.trinitybytes.minka.features.auth.application.RequestPasswordReset
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.ContactKind
import com.trinitybytes.minka.features.auth.domain.contactKindOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** One-off feedback shown in a snackbar; [Offline] (1.22) offers "Reintentar" over the kept step. */
enum class RecoveryMessage { Offline, Unexpected }

/** The link was sent to [destination] (Figma 1.11). */
data class RecoverySent(val destination: String, val kind: ContactKind, val expiresInMinutes: Int)

/**
 * Every state of Figma 1.10–1.11: the single contact field (1.10) and, once [sent] is set, the
 * confirmation whose "Reenviar" waits for [resendSecondsLeft] to run out (1.11), and no connection
 * (1.22) as a snackbar over the untouched step.
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

private const val KEY_CONTACT = "recovery_contact"
private const val KEY_DESTINATION = "recovery_destination"
private const val KEY_KIND = "recovery_kind"
private const val KEY_EXPIRY = "recovery_expiry"
private const val KEY_DEADLINE = "recovery_deadline"

@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val requestPasswordReset: RequestPasswordReset,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PasswordRecoveryUiState(
            contact = savedStateHandle[KEY_CONTACT] ?: "",
            sent = savedStateHandle.get<String>(KEY_DESTINATION)?.let { destination ->
                val kind = savedStateHandle.get<String>(KEY_KIND)?.let { runCatching { ContactKind.valueOf(it) }.getOrNull() }
                val expiry = savedStateHandle.get<Int>(KEY_EXPIRY)
                if (kind != null && expiry != null) RecoverySent(destination, kind, expiry) else null
            },
        ),
    )
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    private var countdown: Job? = null

    init {
        if (_uiState.value.sent != null) savedStateHandle.get<Long>(KEY_DEADLINE)?.let(::runCountdown)
    }

    fun onContactChange(value: String) = _uiState.update { state ->
        if (!state.fieldsEnabled) return@update state
        savedStateHandle[KEY_CONTACT] = value
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

    /** 1.22 "Reintentar": the request again with the field as it is now, or the resend once sent. */
    fun retry() {
        if (_uiState.value.sent == null) submit() else resend()
    }

    private fun send(contact: String) {
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            when (val result = requestPasswordReset(contact)) {
                is Outcome.Success -> {
                    val reset = result.value
                    val kind = contactKindOf(reset.destination) ?: contactKindOf(contact) ?: ContactKind.Email
                    savedStateHandle[KEY_DESTINATION] = reset.destination
                    savedStateHandle[KEY_KIND] = kind.name
                    savedStateHandle[KEY_EXPIRY] = reset.expiresInMinutes
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sent = RecoverySent(reset.destination, kind, reset.expiresInMinutes),
                        )
                    }
                    startCountdown(reset.resendAfterSeconds.toLong())
                }
                is Outcome.Failure -> _uiState.update {
                    val message = if (result.error == AuthError.Offline) RecoveryMessage.Offline else RecoveryMessage.Unexpected
                    it.copy(isLoading = false, message = message)
                }
            }
        }
    }

    /** Same timing as the 1.08 lock: one tick per second, cleared when it reaches zero. */
    private fun startCountdown(seconds: Long) {
        val deadline = clock.millis() + seconds.coerceAtLeast(0) * 1_000L
        savedStateHandle[KEY_DEADLINE] = deadline
        runCountdown(deadline)
    }

    private fun runCountdown(deadline: Long) {
        countdown?.cancel()
        countdown = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            countdown(deadline, clock).collect { left ->
                _uiState.update { it.copy(resendSecondsLeft = left.takeIf { it > 0 }) }
                if (left == 0L) savedStateHandle.remove<Long>(KEY_DEADLINE)
            }
        }
    }
}
