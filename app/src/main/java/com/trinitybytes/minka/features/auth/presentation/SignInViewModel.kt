package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.GetGoogleAccounts
import com.trinitybytes.minka.features.auth.application.SignIn
import com.trinitybytes.minka.features.auth.application.SignInWithGoogle
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.MIN_PASSWORD_LENGTH
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.contactKindOf
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
import javax.inject.Inject

/** Message shown under a field of the sign-in form (Figma 1.06). */
enum class SignInFieldError { InvalidContact, ShortPassword }

/** One-off feedback shown in a snackbar. */
sealed interface SignInMessage {
    data class WrongCredentials(val attemptsLeft: Int) : SignInMessage
    data object Offline : SignInMessage
    data object Unexpected : SignInMessage
}

sealed interface SignInEvent {
    data object SignedIn : SignInEvent
    data object OpenRecovery : SignInEvent
}

/**
 * Every state of Figma 1.05–1.09: empty (1.05), field errors (1.06), loading (1.07), locked
 * account while [lockSecondsLeft] is set (1.08) and the Google sheet while [googleAccounts] is set (1.09).
 */
data class SignInUiState(
    val contact: String = "",
    val password: String = "",
    val contactError: SignInFieldError? = null,
    val passwordError: SignInFieldError? = null,
    val isLoading: Boolean = false,
    val lockSecondsLeft: Long? = null,
    val googleAccounts: List<GoogleAccount>? = null,
    val message: SignInMessage? = null,
) {
    /** 1.07: the fields are locked while signing in. */
    val fieldsEnabled: Boolean get() = !isLoading

    /** 1.05: "Ingresar" is disabled until both fields are filled. */
    val canSubmit: Boolean get() = !isLoading && contact.isNotBlank() && password.isNotEmpty()
}

/** `m:ss`, as in the 1.08 dialog ("Vuelve a intentarlo en 14:59"). */
fun formatCountdown(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signIn: SignIn,
    private val getGoogleAccounts: GetGoogleAccounts,
    private val signInWithGoogle: SignInWithGoogle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = _events.receiveAsFlow()

    private var started = false
    private var countdown: Job? = null

    /** Called once per screen; with [withGoogle] the account sheet opens right away. */
    fun onStart(withGoogle: Boolean) {
        if (started) return
        started = true
        if (withGoogle) openGoogleSheet()
    }

    fun onContactChange(value: String) = _uiState.update { state ->
        if (!state.fieldsEnabled) return@update state
        // Once a message shows, it follows the typing so it disappears as soon as the value is valid.
        state.copy(contact = value, contactError = state.contactError?.let { contactError(value) })
    }

    fun onPasswordChange(value: String) = _uiState.update { state ->
        if (!state.fieldsEnabled) return@update state
        state.copy(password = value, passwordError = state.passwordError?.let { passwordError(value) })
    }

    /** 1.06: validation happens when the field loses focus; an untouched empty field is not flagged. */
    fun onContactFocusLost() = _uiState.update {
        if (it.contact.isBlank()) it else it.copy(contactError = contactError(it.contact))
    }

    fun onPasswordFocusLost() = _uiState.update {
        if (it.password.isEmpty()) it else it.copy(passwordError = passwordError(it.password))
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val contactError = contactError(state.contact)
        val passwordError = passwordError(state.password)
        if (contactError != null || passwordError != null) {
            _uiState.update { it.copy(contactError = contactError, passwordError = passwordError) }
            return
        }
        authenticate { signIn(state.contact.trim(), state.password) }
    }

    fun openGoogleSheet() {
        viewModelScope.launch {
            when (val result = getGoogleAccounts()) {
                is Outcome.Success -> _uiState.update { it.copy(googleAccounts = result.value) }
                is Outcome.Failure -> _uiState.update { it.copy(message = result.error.toMessage()) }
            }
        }
    }

    fun dismissGoogleSheet() = _uiState.update { it.copy(googleAccounts = null) }

    /** 1.09: choosing an account closes the sheet and moves to the loading state (1.07). */
    fun chooseGoogleAccount(id: String) {
        dismissGoogleSheet()
        authenticate { signInWithGoogle(id) }
    }

    fun dismissLock() {
        countdown?.cancel()
        _uiState.update { it.copy(lockSecondsLeft = null) }
    }

    /** "Recuperar" in the 1.08 dialog and "¿Olvidaste tu contraseña?" lead to password recovery. */
    fun recoverPassword() {
        dismissLock()
        _events.trySend(SignInEvent.OpenRecovery)
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun authenticate(request: suspend () -> AuthOutcome<Session>) {
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = request()
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Outcome.Success -> _events.send(SignInEvent.SignedIn)
                is Outcome.Failure -> when (val error = result.error) {
                    is AuthError.AccountLocked -> startLock(error.retryAfterSeconds)
                    else -> _uiState.update { it.copy(message = error.toMessage()) }
                }
            }
        }
    }

    /** 1.08: the dialog counts down every second and closes when the lock ends. */
    private fun startLock(seconds: Long) {
        countdown?.cancel()
        _uiState.update { it.copy(lockSecondsLeft = seconds) }
        countdown = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1_000)
                left--
                _uiState.update { it.copy(lockSecondsLeft = left.takeIf { s -> s > 0 }) }
            }
        }
    }

    private fun contactError(value: String) = if (contactKindOf(value) == null) SignInFieldError.InvalidContact else null

    private fun passwordError(value: String) =
        if (value.length < MIN_PASSWORD_LENGTH) SignInFieldError.ShortPassword else null

    private fun AuthError.toMessage(): SignInMessage = when (this) {
        is AuthError.InvalidCredentials -> SignInMessage.WrongCredentials(attemptsLeft)
        AuthError.Offline -> SignInMessage.Offline
        else -> SignInMessage.Unexpected
    }
}
