package com.trinitybytes.minka.features.auth.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaExpressiveShapes
import com.trinitybytes.minka.core.designsystem.component.MinkaMessageEffect
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaTextField
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBarSize
import com.trinitybytes.minka.core.designsystem.component.formColumnWidth
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.auth.domain.ContactKind

/** Figma 1.10–1.11. [onBack] returns to sign-in from either state. */
@Composable
fun PasswordRecoveryScreen(
    onBack: () -> Unit,
    viewModel: PasswordRecoveryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    PasswordRecoveryContent(
        uiState = uiState,
        onBack = onBack,
        onContactChange = viewModel::onContactChange,
        onContactFocusLost = viewModel::onContactFocusLost,
        onSubmit = viewModel::submit,
        onOpenInbox = { kind -> context.openInbox(kind) },
        onResend = viewModel::resend,
        onMessageShown = viewModel::onMessageShown,
        onRetry = viewModel::retry,
    )
}

/** One composable for both recovery states, driven by [uiState]. */
@Composable
fun PasswordRecoveryContent(
    uiState: PasswordRecoveryUiState,
    onBack: () -> Unit,
    onContactChange: (String) -> Unit,
    onContactFocusLost: () -> Unit,
    onSubmit: () -> Unit,
    onOpenInbox: (ContactKind) -> Unit,
    onResend: () -> Unit,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // 1.22: without connection the step stays as it was and the snackbar offers "Reintentar".
    MinkaMessageEffect(
        hostState = snackbarHostState,
        message = uiState.message,
        text = uiState.message?.let { messageText(it) },
        onShown = onMessageShown,
        actionLabel = stringResource(R.string.offline_retry).takeIf { uiState.message == RecoveryMessage.Offline },
        onAction = onRetry,
    )
    val sent = uiState.sent
    // Design B5: top app bar and form share one centered column of at most 480 dp.
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Scaffold(
            modifier = Modifier.formColumnWidth(),
            topBar = {
                // 1.10 uses the large bar with its headline; 1.11 a small bar with only the back arrow.
                if (sent == null) {
                    MinkaTopAppBar(
                        title = stringResource(R.string.password_recovery_title),
                        size = MinkaTopAppBarSize.Large,
                        onBack = onBack,
                    )
                } else {
                    MinkaTopAppBar(title = "", onBack = onBack)
                }
            },
            snackbarHost = { MinkaSnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .padding(start = MinkaSpacing.s16, end = MinkaSpacing.s16, top = MinkaSpacing.s8, bottom = MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                if (sent == null) {
                    RequestForm(uiState, onContactChange, onContactFocusLost, onSubmit)
                } else {
                    SentConfirmation(uiState, sent, onOpenInbox, onResend)
                }
            }
        }
    }
}

/** 1.10: one field that accepts an email or a phone. */
@Composable
private fun ColumnScope.RequestForm(
    uiState: PasswordRecoveryUiState,
    onContactChange: (String) -> Unit,
    onContactFocusLost: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
    ) {
        Text(
            text = stringResource(R.string.password_recovery_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val focusLost by rememberUpdatedState(onContactFocusLost)
        val hadFocus = remember { BooleanArray(1) }
        MinkaTextField(
            value = uiState.contact,
            onValueChange = onContactChange,
            label = stringResource(R.string.password_recovery_contact_label),
            modifier = Modifier.onFocusChanged { state ->
                if (hadFocus[0] && !state.hasFocus) focusLost()
                hadFocus[0] = state.hasFocus
            },
            supportingText = if (uiState.contactError) stringResource(R.string.sign_in_contact_error) else null,
            isError = uiState.contactError,
            enabled = uiState.fieldsEnabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
        )
    }
    MinkaButton(
        text = stringResource(R.string.password_recovery_submit),
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.canSubmit,
        size = MinkaButtonSize.Large,
        loading = uiState.isLoading,
    )
}

/** 1.11: the confirmation; "Reenviar" waits for the countdown. */
@Composable
private fun ColumnScope.SentConfirmation(
    uiState: PasswordRecoveryUiState,
    sent: RecoverySent,
    onOpenInbox: (ContactKind) -> Unit,
    onResend: () -> Unit,
) {
    val isPhone = sent.kind == ContactKind.Phone
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MinkaTheme.colors.sky, MinkaExpressiveShapes.Cookie12Compact),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPhone) Icons.Rounded.MarkChatRead else Icons.Rounded.MarkEmailRead,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MinkaTheme.colors.onSky,
            )
        }
        Text(
            text = stringResource(
                if (isPhone) R.string.password_recovery_sent_phone_title else R.string.password_recovery_sent_email_title,
            ),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (isPhone) {
                stringResource(R.string.password_recovery_sent_phone_text, groupPhoneDigits(sent.destination), sent.expiresInMinutes)
            } else {
                stringResource(R.string.password_recovery_sent_text, sent.destination, sent.expiresInMinutes)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        MinkaButton(
            text = stringResource(
                if (isPhone) R.string.password_recovery_open_messages else R.string.password_recovery_open_email,
            ),
            onClick = { onOpenInbox(sent.kind) },
            modifier = Modifier.fillMaxWidth(),
            size = MinkaButtonSize.Large,
        )
        MinkaButton(
            text = uiState.resendSecondsLeft
                ?.let { stringResource(R.string.password_recovery_resend_in, formatCountdown(it)) }
                ?: stringResource(R.string.password_recovery_resend),
            onClick = onResend,
            modifier = Modifier.fillMaxWidth(),
            style = MinkaButtonStyle.Text,
            enabled = uiState.canResend,
            size = MinkaButtonSize.Large,
            loading = uiState.isLoading,
        )
    }
}

@Composable
private fun messageText(message: RecoveryMessage): String = when (message) {
    RecoveryMessage.Offline -> stringResource(R.string.offline_snackbar)
    RecoveryMessage.Unexpected -> stringResource(R.string.sign_in_unexpected)
}

/** "Abrir correo" opens the email app; for a phone, the messages app. Nothing happens if there is none. */
private fun Context.openInbox(kind: ContactKind) {
    val category = if (kind == ContactKind.Phone) Intent.CATEGORY_APP_MESSAGING else Intent.CATEGORY_APP_EMAIL
    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No inbox app on the device; the user can still open it by hand.
    }
}

@Composable
private fun PasswordRecoveryPreview(uiState: PasswordRecoveryUiState) {
    MinkaTheme {
        PasswordRecoveryContent(
            uiState = uiState,
            onBack = {},
            onContactChange = {},
            onContactFocusLost = {},
            onSubmit = {},
            onOpenInbox = {},
            onResend = {},
            onMessageShown = {},
            onRetry = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PasswordRecoveryRequestPreview() =
    PasswordRecoveryPreview(PasswordRecoveryUiState(contact = "ana.quispe@gmail.com"))

@PreviewLightDark
@Composable
private fun PasswordRecoverySentPreview() = PasswordRecoveryPreview(
    PasswordRecoveryUiState(
        contact = "ana.quispe@gmail.com",
        sent = RecoverySent("ana.quispe@gmail.com", ContactKind.Email, 30),
        resendSecondsLeft = 45,
    ),
)
