package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaCheckboxRow
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaStrengthMeter
import com.trinitybytes.minka.core.designsystem.component.MinkaTextField
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.MinkaWavyProgress
import com.trinitybytes.minka.core.designsystem.component.StrengthLevel
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.auth.domain.ContactKind
import com.trinitybytes.minka.features.auth.domain.PasswordStrength
import com.trinitybytes.minka.features.auth.domain.TERMS_VERSION
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import com.trinitybytes.minka.features.auth.domain.contactKindOf

/** Figma 1.12–1.13. [onCodeSent] continues to the code verification (1.14). */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onCodeSent: (VerificationChallenge) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val codeSent by rememberUpdatedState(onCodeSent)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RegisterEvent.CodeSent -> codeSent(event.challenge)
            }
        }
    }
    RegisterContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onContactChange = viewModel::onContactChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onTermsChange = viewModel::onTermsChange,
        onContactFocusLost = viewModel::onContactFocusLost,
        onPasswordFocusLost = viewModel::onPasswordFocusLost,
        onConfirmPasswordFocusLost = viewModel::onConfirmPasswordFocusLost,
        onSubmit = viewModel::submit,
        onMessageShown = viewModel::onMessageShown,
    )
}

/** One composable for both registration states, driven by [uiState]. */
@Composable
fun RegisterContent(
    uiState: RegisterUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onContactFocusLost: () -> Unit,
    onPasswordFocusLost: () -> Unit,
    onConfirmPasswordFocusLost: () -> Unit,
    onSubmit: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { registerMessageText(it) }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = { MinkaTopAppBar(title = "", onBack = onBack) },
        snackbarHost = { MinkaSnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(start = MinkaSpacing.s16, end = MinkaSpacing.s16, top = MinkaSpacing.s8, bottom = MinkaSpacing.s16),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                AccountStepHeader(title = stringResource(R.string.register_title))
                RegisterForm(
                    uiState,
                    onNameChange,
                    onContactChange,
                    onPasswordChange,
                    onConfirmPasswordChange,
                    onContactFocusLost,
                    onPasswordFocusLost,
                    onConfirmPasswordFocusLost,
                )
                Terms(uiState, onTermsChange)
            }
            MinkaButton(
                text = stringResource(R.string.register_submit),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit,
                size = MinkaButtonSize.Large,
                loading = uiState.isLoading,
            )
        }
    }
}

/** The wavy "Paso 1 de 3 · Cuenta" progress and the headline, shared by 1.12–1.15. */
@Composable
internal fun AccountStepHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        MinkaWavyProgress(progress = 1f / 3)
        Text(
            text = stringResource(R.string.register_step),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(text = title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun RegisterForm(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onContactFocusLost: () -> Unit,
    onPasswordFocusLost: () -> Unit,
    onConfirmPasswordFocusLost: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12)) {
        MinkaTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.register_name_label),
            enabled = uiState.fieldsEnabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        MinkaTextField(
            value = uiState.contact,
            onValueChange = onContactChange,
            label = stringResource(R.string.register_contact_label),
            modifier = Modifier.onFocusLost(onContactFocusLost),
            supportingText = uiState.contactError?.let { fieldErrorText(it, uiState.contact) },
            isError = uiState.contactError != null,
            enabled = uiState.fieldsEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            MinkaTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.register_password_label),
                modifier = Modifier.onFocusLost(onPasswordFocusLost),
                supportingText = uiState.passwordError?.let { fieldErrorText(it, uiState.contact) },
                isError = uiState.passwordError != null,
                enabled = uiState.fieldsEnabled,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )
            uiState.strength?.let { MinkaStrengthMeter(level = it.toLevel(), modifier = Modifier.fillMaxWidth()) }
        }
        MinkaTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.register_confirm_password_label),
            modifier = Modifier.onFocusLost(onConfirmPasswordFocusLost),
            supportingText = uiState.confirmError?.let { fieldErrorText(it, uiState.contact) },
            isError = uiState.confirmError != null,
            enabled = uiState.fieldsEnabled,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
    }
}

/** "Acepto los términos y la privacidad (v1.0)" and, in 1.13, its error below. */
@Composable
private fun Terms(uiState: RegisterUiState, onTermsChange: (Boolean) -> Unit) {
    Column {
        MinkaCheckboxRow(
            checked = uiState.termsAccepted,
            onCheckedChange = onTermsChange,
            label = stringResource(R.string.register_terms, TERMS_VERSION),
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.fieldsEnabled,
        )
        if (uiState.termsError) {
            Row(
                modifier = Modifier.padding(start = MinkaSpacing.s12),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Error,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.register_terms_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun fieldErrorText(error: RegisterFieldError, contact: String): String = when (error) {
    RegisterFieldError.InvalidContact -> stringResource(R.string.sign_in_contact_error)
    RegisterFieldError.ContactTaken -> stringResource(
        if (contactKindOf(contact) == ContactKind.Phone) R.string.register_phone_taken else R.string.register_email_taken,
    )
    RegisterFieldError.ShortPassword -> stringResource(R.string.sign_in_password_error)
    RegisterFieldError.WeakPassword -> stringResource(R.string.register_weak_password)
    RegisterFieldError.PasswordMismatch -> stringResource(R.string.register_password_mismatch)
}

@Composable
private fun registerMessageText(message: RegisterMessage): String = when (message) {
    RegisterMessage.Offline -> stringResource(R.string.sign_in_offline)
    RegisterMessage.Unexpected -> stringResource(R.string.sign_in_unexpected)
}

private fun PasswordStrength.toLevel(): StrengthLevel = when (this) {
    PasswordStrength.Weak -> StrengthLevel.Weak
    PasswordStrength.Medium -> StrengthLevel.Medium
    PasswordStrength.Strong -> StrengthLevel.Strong
}

/** Calls [onLost] when the field had focus and loses it (1.13: "Errores por campo al salir de cada uno"). */
@Composable
private fun Modifier.onFocusLost(onLost: () -> Unit): Modifier {
    // Remembered so a recomposition while typing does not forget that the field had focus.
    val hadFocus = remember { BooleanArray(1) }
    val lost by rememberUpdatedState(onLost)
    return onFocusChanged { state ->
        if (hadFocus[0] && !state.hasFocus) lost()
        hadFocus[0] = state.hasFocus
    }
}

@Composable
private fun RegisterPreview(uiState: RegisterUiState) {
    MinkaTheme {
        RegisterContent(
            uiState = uiState,
            onBack = {},
            onNameChange = {},
            onContactChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTermsChange = {},
            onContactFocusLost = {},
            onPasswordFocusLost = {},
            onConfirmPasswordFocusLost = {},
            onSubmit = {},
            onMessageShown = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RegisterDataPreview() = RegisterPreview(
    RegisterUiState(
        name = "Ana Quispe",
        contact = "ana.quispe@gmail.com",
        password = "minka2026",
        confirmPassword = "minka2026",
        termsAccepted = true,
    ),
)

@PreviewLightDark
@Composable
private fun RegisterErrorsPreview() = RegisterPreview(
    RegisterUiState(
        name = "Ana Quispe",
        contact = "ana.quispe@gmail.com",
        password = "minka2026",
        confirmPassword = "minka2025",
        contactError = RegisterFieldError.ContactTaken,
        confirmError = RegisterFieldError.PasswordMismatch,
        termsError = true,
    ),
)
