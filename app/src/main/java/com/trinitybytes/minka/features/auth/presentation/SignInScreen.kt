package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaBottomSheet
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaDialog
import com.trinitybytes.minka.core.designsystem.component.MinkaListGroup
import com.trinitybytes.minka.core.designsystem.component.MinkaListItem
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaTextField
import com.trinitybytes.minka.core.designsystem.component.MinkaOfflineScreen
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBarSize
import com.trinitybytes.minka.core.designsystem.component.initialsOf
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.onboarding.domain.SetupStep

/** Figma 1.05–1.09. [withGoogle] opens the Google account sheet (1.09) right away. */
@Composable
fun SignInScreen(
    withGoogle: Boolean,
    onBack: () -> Unit,
    /** Receives the profile setup step to resume, or null to open Inicio. */
    onSignedIn: (pendingStep: SetupStep?) -> Unit,
    onRecoverPassword: () -> Unit,
    onRegister: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signedIn by rememberUpdatedState(onSignedIn)
    val recover by rememberUpdatedState(onRecoverPassword)
    LaunchedEffect(viewModel) {
        viewModel.onStart(withGoogle)
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.SignedIn -> signedIn(event.pendingStep)
                SignInEvent.OpenRecovery -> recover()
            }
        }
    }
    SignInContent(
        uiState = uiState,
        onBack = onBack,
        onContactChange = viewModel::onContactChange,
        onPasswordChange = viewModel::onPasswordChange,
        onContactFocusLost = viewModel::onContactFocusLost,
        onPasswordFocusLost = viewModel::onPasswordFocusLost,
        onSubmit = viewModel::submit,
        onForgotPassword = viewModel::recoverPassword,
        onGoogle = viewModel::openGoogleSheet,
        onRegister = onRegister,
        onChooseGoogleAccount = viewModel::chooseGoogleAccount,
        onDismissGoogle = viewModel::dismissGoogleSheet,
        onDismissLock = viewModel::dismissLock,
        onMessageShown = viewModel::onMessageShown,
        onRetry = viewModel::retry,
    )
}

/** One composable for every sign-in state, driven by [uiState]. */
@Composable
fun SignInContent(
    uiState: SignInUiState,
    onBack: () -> Unit,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContactFocusLost: () -> Unit,
    onPasswordFocusLost: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogle: () -> Unit,
    onRegister: () -> Unit,
    onChooseGoogleAccount: (String) -> Unit,
    onDismissGoogle: () -> Unit,
    onDismissLock: () -> Unit,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 1.22: no connection replaces the step; the typed values stay in [uiState] for the retry.
    if (uiState.isOffline) {
        MinkaOfflineScreen(onBack = onBack, onRetry = onRetry, modifier = modifier, retrying = uiState.isLoading)
        return
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { messageText(it) }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            MinkaTopAppBar(
                title = stringResource(R.string.sign_in_title),
                size = MinkaTopAppBarSize.Large,
                onBack = onBack,
            )
        },
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
                Text(
                    text = stringResource(R.string.sign_in_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SignInForm(uiState, onContactChange, onPasswordChange, onContactFocusLost, onPasswordFocusLost, onSubmit)
                MinkaButton(
                    text = stringResource(R.string.sign_in_forgot_password),
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End),
                    style = MinkaButtonStyle.Text,
                    enabled = uiState.fieldsEnabled,
                )
            }
            SignInActions(uiState, onSubmit, onGoogle, onRegister)
        }
    }
    uiState.lockSecondsLeft?.let { seconds ->
        MinkaDialog(
            title = stringResource(R.string.sign_in_locked_title),
            text = stringResource(R.string.sign_in_locked_text, formatCountdown(seconds)),
            confirmText = stringResource(R.string.sign_in_locked_ok),
            onConfirm = onDismissLock,
            onDismiss = onDismissLock,
            dismissText = stringResource(R.string.sign_in_locked_recover),
            onDismissClick = onForgotPassword,
            icon = Icons.Rounded.LockClock,
            iconTint = MaterialTheme.colorScheme.tertiary,
        )
    }
    uiState.googleAccounts?.let { accounts ->
        GoogleAccountSheet(accounts, onChooseGoogleAccount, onDismissGoogle)
    }
}

@Composable
private fun SignInForm(
    uiState: SignInUiState,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContactFocusLost: () -> Unit,
    onPasswordFocusLost: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12)) {
        MinkaTextField(
            value = uiState.contact,
            onValueChange = onContactChange,
            label = stringResource(R.string.sign_in_contact_label),
            modifier = Modifier.onFocusLost(onContactFocusLost),
            placeholder = stringResource(R.string.sign_in_contact_placeholder),
            supportingText = uiState.contactError?.let { stringResource(R.string.sign_in_contact_error) },
            isError = uiState.contactError != null,
            enabled = uiState.fieldsEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        MinkaTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.sign_in_password_label),
            modifier = Modifier.onFocusLost(onPasswordFocusLost),
            placeholder = stringResource(R.string.sign_in_password_placeholder),
            supportingText = uiState.passwordError?.let { stringResource(R.string.sign_in_password_error) },
            isError = uiState.passwordError != null,
            enabled = uiState.fieldsEnabled,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
    }
}

@Composable
private fun SignInActions(uiState: SignInUiState, onSubmit: () -> Unit, onGoogle: () -> Unit, onRegister: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16)) {
        MinkaButton(
            text = stringResource(R.string.sign_in_submit),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.canSubmit,
            size = MinkaButtonSize.Large,
            loading = uiState.isLoading,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.sign_in_or),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        MinkaButton(
            text = stringResource(R.string.sign_in_google),
            onClick = onGoogle,
            modifier = Modifier.fillMaxWidth(),
            style = MinkaButtonStyle.Outlined,
            enabled = uiState.fieldsEnabled,
            size = MinkaButtonSize.Large,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sign_in_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MinkaButton(
                text = stringResource(R.string.sign_in_register),
                onClick = onRegister,
                style = MinkaButtonStyle.Text,
                enabled = uiState.fieldsEnabled,
            )
        }
    }
}

/** Figma 1.09: the mock picker has no real device accounts, so "Agregar otra cuenta" just closes it. */
@Composable
private fun GoogleAccountSheet(accounts: List<GoogleAccount>, onChoose: (String) -> Unit, onDismiss: () -> Unit) {
    MinkaBottomSheet(title = stringResource(R.string.sign_in_google_title), onDismiss = onDismiss) {
        MinkaListGroup {
            accounts.forEach { account ->
                MinkaListItem(
                    headline = account.name,
                    supporting = account.email,
                    avatarInitials = initialsOf(account.name),
                    onClick = { onChoose(account.id) },
                )
            }
            MinkaListItem(
                headline = stringResource(R.string.sign_in_google_add_account),
                leadingIcon = Icons.Rounded.PersonAdd,
                trailingIcon = Icons.Rounded.ChevronRight,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun messageText(message: SignInMessage): String = when (message) {
    is SignInMessage.WrongCredentials ->
        pluralStringResource(R.plurals.sign_in_wrong_credentials, message.attemptsLeft, message.attemptsLeft)
    SignInMessage.Unexpected -> stringResource(R.string.sign_in_unexpected)
}

/** Calls [onLost] when the field had focus and loses it (1.06: "Validación al salir del campo"). */
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
private fun SignInPreview(uiState: SignInUiState) {
    MinkaTheme {
        SignInContent(
            uiState = uiState,
            onBack = {},
            onContactChange = {},
            onPasswordChange = {},
            onContactFocusLost = {},
            onPasswordFocusLost = {},
            onSubmit = {},
            onForgotPassword = {},
            onGoogle = {},
            onRegister = {},
            onChooseGoogleAccount = {},
            onDismissGoogle = {},
            onDismissLock = {},
            onMessageShown = {},
            onRetry = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SignInEmptyPreview() = SignInPreview(SignInUiState())

@PreviewLightDark
@Composable
private fun SignInErrorPreview() = SignInPreview(
    SignInUiState(
        contact = "ana.quispe@",
        password = "1234",
        contactError = SignInFieldError.InvalidContact,
        passwordError = SignInFieldError.ShortPassword,
    ),
)

@PreviewLightDark
@Composable
private fun SignInLoadingPreview() =
    SignInPreview(SignInUiState(contact = "ana.quispe@gmail.com", password = "Minka2026!", isLoading = true))

@PreviewLightDark
@Composable
private fun SignInLockedPreview() =
    SignInPreview(SignInUiState(contact = "ana.quispe@gmail.com", password = "Minka2026", lockSecondsLeft = 899))
