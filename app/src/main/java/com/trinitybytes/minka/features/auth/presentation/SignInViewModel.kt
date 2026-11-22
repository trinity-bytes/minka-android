package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.GetGoogleAccounts
import com.trinitybytes.minka.features.auth.application.SignIn
import com.trinitybytes.minka.features.auth.application.SignInWithGoogle
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.contactKindOf
import com.trinitybytes.minka.features.onboarding.application.LaunchTarget
import com.trinitybytes.minka.features.onboarding.application.ResolveLaunch
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
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

/**
 * Message shown under a field of the sign-in form (Figma 1.06). Only the contact format is checked:
 * the password is never validated by length at sign-in (design B1), wrong credentials say so.
 */
enum class SignInFieldError { InvalidContact }

/** One-off feedback shown in a snackbar. */
sealed interface SignInMessage {
    data class WrongCredentials(val attemptsLeft: Int) : SignInMessage

    /** 1.22: "Sin conexión. Lo escrito sigue aquí." with "Reintentar"; the form stays as it was. */
    data object Offline : SignInMessage
    data object Unexpected : SignInMessage
}

sealed interface SignInEvent {
    /** Signed in; [pendingStep] is the profile setup step this account left pending on the device, if any. */
    data class SignedIn(val pendingStep: SetupStep?) : SignInEvent
    data object OpenRecovery : SignInEvent
}

/**
 * Every state of Figma 1.05–1.09: empty (1.05), field errors (1.06), loading (1.07), locked
 * account while [lockSecondsLeft] is set (1.08), the Google sheet while [googleAccounts] is set (1.09)
 * and no connection (1.22) as a [SignInMessage.Offline] snackbar over the untouched form.
 */
data class SignInUiState(
    val contact: String = "",
    val password: String = "",
    val contactError: SignInFieldError? = null,
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
    private val resolveLaunch: ResolveLaunch,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState(contact = savedStateHandle["signin_contact"] ?: ""))
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = _events.receiveAsFlow()

    private var started = false
    private var countdown: Job? = null

    /** What "Reintentar" (1.22) sends again, with what the form holds when it is tapped. */
    private var retryAction: RetryAction? = null

    /** Called once per screen; with [withGoogle] the account sheet opens right away. */
    fun onStart(withGoogle: Boolean) {
        if (started) return
        started = true
        if (withGoogle) openGoogleSheet()
    }

    fun onContactChange(value: String) = _uiState.update { state ->
        if (!state.fieldsEnabled) return@update state
        savedStateHandle["signin_contact"] = value
        // Once a message shows, it follows the typing so it disappears as soon as the value is valid.
        state.copy(contact = value, contactError = state.contactError?.let { contactError(value) })
    }

    fun onPasswordChange(value: String) = _uiState.update { state ->
        if (!state.fieldsEnabled) return@update state
        state.copy(password = value)
    }

    /** 1.06: validation happens when the field loses focus; an untouched empty field is not flagged. */
    fun onContactFocusLost() = _uiState.update {
        if (it.contact.isBlank()) it else it.copy(contactError = contactError(it.contact))
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val contactError = contactError(state.contact)
        if (contactError != null) {
            _uiState.update { it.copy(contactError = contactError) }
            return
        }
        authenticate(RetryAction.Submit) { signIn(state.contact.trim(), state.password) }
    }

    fun openGoogleSheet() {
        retryAction = RetryAction.GoogleSheet
        _uiState.update { it.copy(message = null) }
        viewModelScope.launch {
            when (val result = getGoogleAccounts()) {
                is Outcome.Success -> _uiState.update { it.copy(googleAccounts = result.value) }
                is Outcome.Failure -> report(result.error)
            }
        }
    }

    fun dismissGoogleSheet() = _uiState.update { it.copy(googleAccounts = null) }

    /** 1.09: choosing an account closes the sheet and moves to the loading state (1.07). */
    fun chooseGoogleAccount(id: String) {
        dismissGoogleSheet()
        authenticate(RetryAction.GoogleAccount(id)) { signInWithGoogle(id) }
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

    /** 1.22 "Reintentar": sends the last request again with what the form holds now. */
    fun retry() {
        if (_uiState.value.isLoading) return
        when (val action = retryAction) {
            RetryAction.Submit -> submit()
            RetryAction.GoogleSheet -> openGoogleSheet()
            is RetryAction.GoogleAccount -> chooseGoogleAccount(action.id)
            null -> Unit
        }
    }

    private fun authenticate(action: RetryAction, request: suspend () -> AuthOutcome<Session>) {
        retryAction = action
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = request()
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Outcome.Success -> _events.send(SignInEvent.SignedIn(pendingStep = pendingSetupStep()))
                is Outcome.Failure -> report(result.error)
            }
        }
    }

    /** The setup step this account left pending on the device (S4), or null when Inicio opens. */
    private suspend fun pendingSetupStep(): SetupStep? = (resolveLaunch() as? LaunchTarget.Setup)?.step

    /** A lock opens the 1.08 dialog; anything else is a snackbar over the form. */
    private fun report(error: AuthError) {
        when (error) {
            is AuthError.AccountLocked -> startLock(error.retryAfterSeconds)
            else -> _uiState.update { it.copy(message = error.toMessage()) }
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

    private fun AuthError.toMessage(): SignInMessage = when (this) {
        is AuthError.InvalidCredentials -> SignInMessage.WrongCredentials(attemptsLeft)
        AuthError.Offline -> SignInMessage.Offline
        else -> SignInMessage.Unexpected
    }

    /** The kinds of request "Reintentar" can send again; none of them keeps stale form values. */
    private sealed interface RetryAction {
        data object Submit : RetryAction
        data object GoogleSheet : RetryAction
        data class GoogleAccount(val id: String) : RetryAction
    }
}
