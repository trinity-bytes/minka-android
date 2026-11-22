package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.ObserveAsEvents
import com.trinitybytes.minka.core.designsystem.component.MinkaBottomSheet
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaDialog
import com.trinitybytes.minka.core.designsystem.component.MinkaGoogleButton
import com.trinitybytes.minka.core.designsystem.component.MinkaListGroup
import com.trinitybytes.minka.core.designsystem.component.MinkaListItem
import com.trinitybytes.minka.core.designsystem.component.MinkaMessageEffect
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaTextField
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBarSize
import com.trinitybytes.minka.core.designsystem.component.formColumnWidth
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
    val signedIn by rememberUpdatedState(onSignedIn)
    val recover by rememberUpdatedState(onRecoverPassword)
    LaunchedEffect(viewModel) { viewModel.onStart(withGoogle) }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SignInEvent.SignedIn -> signedIn(event.pendingStep)
            SignInEvent.OpenRecovery -> recover()
        }
    }
    SignInStateCollector(viewModel.uiState, lock = { countdownState ->
        SignInLiveLockDialog(countdownState, viewModel::dismissLock, viewModel::recoverPassword)
    }) { uiState ->
        SignInContent(
            uiState = uiState,
            onBack = onBack,
            onContactChange = viewModel::onContactChange,
            onPasswordChange = viewModel::onPasswordChange,
            onContactFocusLost = viewModel::onContactFocusLost,
            onSubmit = viewModel::submit,
            onForgotPassword = viewModel::recoverPassword,
            onGoogle = viewModel::openGoogleSheet,
            onRegister = onRegister,
            onChooseGoogleAccount = viewModel::chooseGoogleAccount,
            onDismissGoogle = viewModel::dismissGoogleSheet,
            onDismissLock = viewModel::dismissLock,
            onMessageShown = viewModel::onMessageShown,
            onRetry = viewModel::retry,
            showLockDialog = false,
        )
    }
}

/** One composable for every sign-in state, driven by [uiState]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInContent(
    uiState: SignInUiState,
    onBack: () -> Unit,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContactFocusLost: () -> Unit,
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
    showLockDialog: Boolean = true,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    // 1.22: without connection the form stays as typed and the snackbar offers "Reintentar".
    MinkaMessageEffect(
        hostState = snackbarHostState,
        message = uiState.message,
        text = uiState.message?.let { messageText(it) },
        onShown = onMessageShown,
        actionLabel = stringResource(R.string.offline_retry).takeIf { uiState.message == SignInMessage.Offline },
        onAction = onRetry,
    )
    // Design B5: top app bar and form share one centered column of at most 480 dp.
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Scaffold(
            modifier = Modifier.formColumnWidth().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MinkaTopAppBar(
                    title = stringResource(R.string.sign_in_title),
                    size = MinkaTopAppBarSize.Large,
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                )
            },
            snackbarHost = { MinkaSnackbarHost(snackbarHostState) },
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding(),
            ) {
                val viewportHeight = maxHeight
                // The actions scroll with the form, so they stay reachable in landscape with the
                // keyboard open; when everything fits they sit at the bottom.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(min = viewportHeight)
                            .padding(start = MinkaSpacing.s16, end = MinkaSpacing.s16, top = MinkaSpacing.s8, bottom = MinkaSpacing.s16),
                        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
                    ) {
                        Text(
                            text = stringResource(R.string.sign_in_intro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SignInForm(uiState, onContactChange, onPasswordChange, onContactFocusLost, onSubmit)
                        MinkaButton(
                            text = stringResource(R.string.sign_in_forgot_password),
                            onClick = onForgotPassword,
                            modifier = Modifier.align(Alignment.End),
                            style = MinkaButtonStyle.Text,
                            enabled = uiState.fieldsEnabled,
                        )
                        Spacer(Modifier.weight(1f))
                        SignInActions(uiState, onSubmit, onGoogle, onRegister)
                    }
                }
            }
        }
    }
    if (showLockDialog) {
        uiState.lockSecondsLeft?.let { seconds -> SignInLockDialog(seconds, onDismissLock, onForgotPassword) }
    }
    uiState.googleAccounts?.let { accounts ->
        GoogleAccountSheet(accounts, onChooseGoogleAccount, onDismissGoogle)
    }
}

/** Keep the lock transition, but not each countdown tick, in the form's state. */
internal fun SignInUiState.withoutCountdownTicks(): SignInUiState =
    copy(lockSecondsLeft = lockSecondsLeft?.let { 1L })

@Composable
internal fun SignInStateCollector(
    state: StateFlow<SignInUiState>,
    lock: @Composable (StateFlow<SignInUiState>) -> Unit,
    form: @Composable (SignInUiState) -> Unit,
) {
    val formFlow = remember(state) { state.map { it.withoutCountdownTicks() }.distinctUntilChanged() }
    val formState by formFlow.collectAsStateWithLifecycle(initialValue = state.value.withoutCountdownTicks())
    form(formState)
    if (formState.lockSecondsLeft != null) lock(state)
}

@Composable
internal fun SignInCountdownText(state: StateFlow<SignInUiState>) {
    val countdown = remember(state) { state.map { it.lockSecondsLeft }.distinctUntilChanged() }
    val seconds by countdown.collectAsStateWithLifecycle(initialValue = state.value.lockSecondsLeft)
    seconds?.let {
        Text(
            text = stringResource(R.string.sign_in_locked_text, formatCountdown(it)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SignInLiveLockDialog(state: StateFlow<SignInUiState>, onDismiss: () -> Unit, onRecover: () -> Unit) {
    MinkaDialog(
        title = stringResource(R.string.sign_in_locked_title),
        text = "",
        confirmText = stringResource(R.string.sign_in_locked_ok),
        onConfirm = onDismiss,
        confirmStyle = MinkaButtonStyle.Text,
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.sign_in_locked_recover),
        onDismissClick = onRecover,
        icon = Icons.Rounded.LockClock,
        iconTint = MaterialTheme.colorScheme.tertiary,
        textContent = { SignInCountdownText(state) },
    )
}

@Composable
private fun SignInLockDialog(seconds: Long, onDismiss: () -> Unit, onRecover: () -> Unit) {
    MinkaDialog(
        title = stringResource(R.string.sign_in_locked_title),
        text = stringResource(R.string.sign_in_locked_text, formatCountdown(seconds)),
        confirmText = stringResource(R.string.sign_in_locked_ok),
        onConfirm = onDismiss,
        confirmStyle = MinkaButtonStyle.Text,
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.sign_in_locked_recover),
        onDismissClick = onRecover,
        icon = Icons.Rounded.LockClock,
        iconTint = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun SignInForm(
    uiState: SignInUiState,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onContactFocusLost: () -> Unit,
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
            placeholder = stringResource(R.string.sign_in_password_placeholder),
            enabled = uiState.fieldsEnabled,
            secure = true,
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
        MinkaGoogleButton(
            text = stringResource(R.string.sign_in_google),
            onClick = onGoogle,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.fieldsEnabled,
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
    SignInMessage.Offline -> stringResource(R.string.offline_snackbar)
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

@Preview
@Composable
private fun SignInEmptyPreview() = SignInPreview(SignInUiState())

@Preview
@Composable
private fun SignInErrorPreview() = SignInPreview(
    SignInUiState(
        contact = "ana.quispe@",
        password = "1234",
        contactError = SignInFieldError.InvalidContact,
    ),
)

@Preview
@Composable
private fun SignInLoadingPreview() =
    SignInPreview(SignInUiState(contact = "ana.quispe@gmail.com", password = "Minka2026!", isLoading = true))

@Preview
@Composable
private fun SignInLockedPreview() =
    SignInPreview(SignInUiState(contact = "ana.quispe@gmail.com", password = "Minka2026", lockSecondsLeft = 899))
