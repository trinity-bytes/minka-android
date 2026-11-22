package com.trinitybytes.minka.features.auth.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.Register
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.MIN_PASSWORD_LENGTH
import com.trinitybytes.minka.features.auth.domain.PasswordStrength
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import com.trinitybytes.minka.features.auth.domain.contactKindOf
import com.trinitybytes.minka.features.auth.domain.isAcceptablePassword
import com.trinitybytes.minka.features.auth.domain.passwordStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Message shown under a field of the registration form (Figma 1.13). */
enum class RegisterFieldError { InvalidContact, ContactTaken, ShortPassword, WeakPassword, PasswordMismatch }

/** One-off feedback shown in a snackbar; [Offline] (1.22) offers "Reintentar" over the kept form. */
enum class RegisterMessage { Offline, Unexpected }

sealed interface RegisterEvent {
    /** The code was sent; the flow continues with its verification (1.14). */
    data class CodeSent(val challenge: VerificationChallenge) : RegisterEvent
}

/**
 * Every state of Figma 1.12–1.13: the form with the live strength meter (1.12) and the per-field
 * errors, shown when each field is left (1.13), and no connection (1.22) as a snackbar over the
 * untouched form.
 */
data class RegisterUiState(
    val name: String = "",
    val contact: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,
    val contactError: RegisterFieldError? = null,
    val passwordError: RegisterFieldError? = null,
    val confirmError: RegisterFieldError? = null,
    val termsError: Boolean = false,
    val isLoading: Boolean = false,
    val message: RegisterMessage? = null,
) {
    val fieldsEnabled: Boolean get() = !isLoading

    /** 1.12: the meter follows the password as it is typed; hidden while the field is empty. */
    val strength: PasswordStrength? get() = password.takeIf { it.isNotEmpty() }?.let(::passwordStrength)

    /** 1.13: "el botón espera a que todo sea válido". */
    val canSubmit: Boolean
        get() = !isLoading &&
            name.isNotBlank() &&
            contactKindOf(contact) != null &&
            contactError != RegisterFieldError.ContactTaken &&
            isAcceptablePassword(password) &&
            confirmPassword == password &&
            termsAccepted
}

private const val KEY_NAME = "register_name"
private const val KEY_CONTACT = "register_contact"
private const val KEY_TERMS = "register_terms"

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: Register,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RegisterUiState(
            name = savedStateHandle[KEY_NAME] ?: "",
            contact = savedStateHandle[KEY_CONTACT] ?: "",
            termsAccepted = savedStateHandle[KEY_TERMS] ?: false,
        ),
    )
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events: Flow<RegisterEvent> = _events.receiveAsFlow()

    // Passwords remain in memory only and are never written to saved state.
    /** Set once the confirmation was left filled in; from then on, a new password rechecks it. */
    private var confirmChecked = false

    fun onNameChange(value: String) = edit {
        savedStateHandle[KEY_NAME] = value
        it.copy(name = value)
    }

    fun onContactChange(value: String) = edit { state ->
        savedStateHandle[KEY_CONTACT] = value
        // Once a message shows, it follows the typing; a taken contact clears as soon as it changes.
        state.copy(contact = value, contactError = state.contactError?.let { contactError(value) })
    }

    fun onPasswordChange(value: String) = edit { state ->
        state.copy(
            password = value,
            passwordError = state.passwordError?.let { passwordError(value) },
            confirmError = if (confirmChecked) confirmError(value, state.confirmPassword) else null,
        )
    }

    fun onConfirmPasswordChange(value: String) = edit { state ->
        state.copy(confirmPassword = value, confirmError = state.confirmError?.let { confirmError(state.password, value) })
    }

    /** Unchecking the terms shows "Debes aceptar los términos"; checking them clears it. */
    fun onTermsChange(accepted: Boolean) = edit {
        savedStateHandle[KEY_TERMS] = accepted
        it.copy(termsAccepted = accepted, termsError = !accepted)
    }

    /** 1.13: validation when a field is left; an untouched empty field is not flagged. */
    fun onContactFocusLost() = _uiState.update {
        if (it.contact.isBlank()) it else it.copy(contactError = it.contactError.takeIfTaken() ?: contactError(it.contact))
    }

    fun onPasswordFocusLost() = _uiState.update {
        if (it.password.isEmpty()) it else it.copy(passwordError = passwordError(it.password))
    }

    /** The confirmation is the last field: leaving it with the terms unchecked also flags them. */
    fun onConfirmPasswordFocusLost() = _uiState.update {
        if (it.confirmPassword.isEmpty()) {
            it
        } else {
            confirmChecked = true
            it.copy(confirmError = confirmError(it.password, it.confirmPassword), termsError = !it.termsAccepted)
        }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = register(state.name.trim(), state.contact.trim(), state.password)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is Outcome.Success -> _events.send(RegisterEvent.CodeSent(result.value))
                is Outcome.Failure -> when (result.error) {
                    AuthError.ContactTaken -> _uiState.update { it.copy(contactError = RegisterFieldError.ContactTaken) }
                    AuthError.Offline -> _uiState.update { it.copy(message = RegisterMessage.Offline) }
                    else -> _uiState.update { it.copy(message = RegisterMessage.Unexpected) }
                }
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** 1.22 "Reintentar": sends the form again with what it holds now ([submit] ignores it while loading). */
    fun retry() = submit()

    /** The fields are locked while the request runs. */
    private fun edit(change: (RegisterUiState) -> RegisterUiState) = _uiState.update { state ->
        if (state.fieldsEnabled) change(state) else state
    }

    private fun RegisterFieldError?.takeIfTaken() = takeIf { it == RegisterFieldError.ContactTaken }

    private fun contactError(value: String) = if (contactKindOf(value) == null) RegisterFieldError.InvalidContact else null

    private fun passwordError(value: String) = when {
        value.length < MIN_PASSWORD_LENGTH -> RegisterFieldError.ShortPassword
        !isAcceptablePassword(value) -> RegisterFieldError.WeakPassword
        else -> null
    }

    private fun confirmError(password: String, confirmation: String) =
        if (confirmation != password) RegisterFieldError.PasswordMismatch else null
}
