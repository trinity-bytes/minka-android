package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.ResendCode
import com.trinitybytes.minka.features.auth.application.VerifyRegistration
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.onboarding.application.BeginOnboarding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** 1.14: "Código de 6 dígitos por correo o SMS." */
const val CODE_LENGTH = 6

/** Message under the code boxes. */
sealed interface CodeError {
    /** 1.15: "Código incorrecto · te quedan 2 intentos". */
    data class Wrong(val attemptsLeft: Int) : CodeError

    /** 1.15: "Al agotar los 5 intentos se pide un código nuevo." */
    data object Exhausted : CodeError
}

/** One-off feedback shown in a snackbar; [Offline] (1.22) offers "Reintentar" over the kept code. */
enum class VerifyMessage { Offline, Unexpected }

sealed interface VerifyEvent {
    /** The account exists and its session is saved; onboarding continues. */
    data object Verified : VerifyEvent
}

/**
 * Every state of Figma 1.14–1.15: the code sent to [destination] with its [attemptsLeft], the wrong
 * code (1.15), "Reenviar código", which waits for [resendSecondsLeft] to run out, and no connection
 * (1.22) as a snackbar over the kept code.
 */
data class VerifyCodeUiState(
    val destination: String,
    val attemptsLeft: Int,
    val code: String = "",
    val codeError: CodeError? = null,
    val isLoading: Boolean = false,
    val resendSecondsLeft: Long? = null,
    val message: VerifyMessage? = null,
) {
    /** "Verificar" waits for the 6 digits; after a wrong code, for an edited one (1.15). */
    val canVerify: Boolean get() = !isLoading && code.length == CODE_LENGTH && codeError == null

    /** 1.14: "reenvío con temporizador". */
    val canResend: Boolean get() = !isLoading && resendSecondsLeft == null
}

@HiltViewModel
class VerifyCodeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val verifyRegistration: VerifyRegistration,
    private val resendCode: ResendCode,
    private val beginOnboarding: BeginOnboarding,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val challengeId: String = checkNotNull(savedStateHandle[ARG_CHALLENGE_ID])

    private val _uiState = MutableStateFlow(
        VerifyCodeUiState(
            destination = checkNotNull(savedStateHandle[ARG_DESTINATION]),
            attemptsLeft = savedStateHandle["verify_attempts"] ?: checkNotNull(savedStateHandle[ARG_ATTEMPTS_LEFT]),
            codeError = if ((savedStateHandle.get<Int>("verify_attempts") ?: 1) <= 0) CodeError.Exhausted else null,
        ),
    )
    val uiState: StateFlow<VerifyCodeUiState> = _uiState.asStateFlow()

    private val _events = Channel<VerifyEvent>(Channel.BUFFERED)
    val events: Flow<VerifyEvent> = _events.receiveAsFlow()

    private var countdown: Job? = null

    /** The last request, which "Reintentar" (1.22) sends again; both read the state when they run. */
    private var lastRequest: (() -> Unit)? = null

    init {
        val deadline = savedStateHandle.get<Long>("verify_deadline") ?: (clock.millis() + checkNotNull(savedStateHandle.get<Int>(ARG_RESEND_AFTER_SECONDS)) * 1_000L).also {
            savedStateHandle["verify_deadline"] = it
        }
        runCountdown(deadline)
    }

    /** Keeps up to 6 digits; editing lifts a wrong-code message, but not the exhausted one. */
    fun onCodeChange(value: String) = _uiState.update { state ->
        if (state.isLoading) return@update state
        val code = value.filter(Char::isDigit).take(CODE_LENGTH)
        state.copy(code = code, codeError = state.codeError.takeIf { it == CodeError.Exhausted })
    }

    fun verify() {
        val state = _uiState.value
        if (!state.canVerify) return
        lastRequest = ::verify
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            // The setup is saved before the session: a crash in between lands on Welcome, never past the setup (S4).
            val result = verifyRegistration(challengeId, state.code) { beginOnboarding(it.user.id) is Outcome.Success }
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Outcome.Success -> _events.send(VerifyEvent.Verified)
                is Outcome.Failure -> when (val error = result.error) {
                    is AuthError.WrongCode -> { savedStateHandle["verify_attempts"] = error.attemptsLeft; _uiState.update {
                        it.copy(attemptsLeft = error.attemptsLeft, codeError = CodeError.Wrong(error.attemptsLeft))
                    } }
                    AuthError.CodeExpired -> { savedStateHandle["verify_attempts"] = 0; _uiState.update { it.copy(attemptsLeft = 0, codeError = CodeError.Exhausted) } }
                    else -> _uiState.update { it.copy(message = error.toMessage()) }
                }
            }
        }
    }

    /** A new code renews the attempts and restarts the countdown. */
    fun resend() {
        if (!_uiState.value.canResend) return
        lastRequest = ::resend
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = resendCode(challengeId)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Outcome.Success -> {
                    val challenge = result.value
                    savedStateHandle["verify_attempts"] = challenge.attemptsLeft
                    _uiState.update { it.copy(code = "", codeError = null, attemptsLeft = challenge.attemptsLeft) }
                    startCountdown(challenge.resendAfterSeconds.toLong())
                }
                is Outcome.Failure -> when (val error = result.error) {
                    is AuthError.TooSoon -> startCountdown(error.retryAfterSeconds)
                    else -> _uiState.update { it.copy(message = error.toMessage()) }
                }
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** 1.22 "Reintentar": sends the last request again with the code the boxes hold now. */
    fun retry() {
        if (_uiState.value.isLoading) return
        lastRequest?.invoke()
    }

    /** Store an absolute deadline; codes themselves remain in memory only. */
    private fun startCountdown(seconds: Long) {
        val deadline = clock.millis() + seconds.coerceAtLeast(0) * 1_000L
        savedStateHandle["verify_deadline"] = deadline
        runCountdown(deadline)
    }

    private fun runCountdown(deadline: Long) {
        countdown?.cancel()
        fun remaining(): Long = ((deadline - clock.millis()).coerceAtLeast(0) + 999) / 1_000
        _uiState.update { it.copy(resendSecondsLeft = remaining().takeIf { left -> left > 0 }) }
        countdown = viewModelScope.launch {
            while (remaining() > 0) {
                delay(1_000)
                _uiState.update { it.copy(resendSecondsLeft = remaining().takeIf { left -> left > 0 }) }
            }
        }
    }

    private fun AuthError.toMessage(): VerifyMessage =
        if (this == AuthError.Offline) VerifyMessage.Offline else VerifyMessage.Unexpected

    companion object {
        // Must match the property names of the navigation route (VerifyCodeRoute).
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_DESTINATION = "destination"
        const val ARG_ATTEMPTS_LEFT = "attemptsLeft"
        const val ARG_RESEND_AFTER_SECONDS = "resendAfterSeconds"
    }
}
