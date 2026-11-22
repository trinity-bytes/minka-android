package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.ObserveAsEvents
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaCodeInput
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaMessageEffect
import com.trinitybytes.minka.core.designsystem.component.formColumnWidth
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma 1.14–1.15. [onVerified] continues the onboarding once the session is saved; back returns
 * to the form, which keeps what was typed.
 */
@Composable
fun VerifyCodeScreen(
    onBack: () -> Unit,
    onVerified: () -> Unit,
    onContactSupport: () -> Unit,
    viewModel: VerifyCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val verified by rememberUpdatedState(onVerified)
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            VerifyEvent.Verified -> verified()
        }
    }
    VerifyCodeContent(
        uiState = uiState,
        onBack = onBack,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::verify,
        onResend = viewModel::resend,
        onContactSupport = onContactSupport,
        onMessageShown = viewModel::onMessageShown,
        onRetry = viewModel::retry,
    )
}

/** One composable for both verification states, driven by [uiState]. */
@Composable
fun VerifyCodeContent(
    uiState: VerifyCodeUiState,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onContactSupport: () -> Unit,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // 1.22: without connection the code stays as typed and the snackbar offers "Reintentar".
    MinkaMessageEffect(
        hostState = snackbarHostState,
        message = uiState.message,
        text = uiState.message?.let { verifyMessageText(it) },
        onShown = onMessageShown,
        actionLabel = stringResource(R.string.offline_retry).takeIf { uiState.message == VerifyMessage.Offline },
        onAction = onRetry,
    )
    // Design B5: the form keeps a centered column of at most 480 dp.
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeContent,
            modifier = Modifier.formColumnWidth(),
            topBar = { MinkaTopAppBar(title = "", onBack = onBack) },
            snackbarHost = { MinkaSnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .padding(start = MinkaSpacing.s16, end = MinkaSpacing.s16, top = MinkaSpacing.s8, bottom = MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
                ) {
                    AccountStepHeader(title = stringResource(R.string.verify_title))
                    Text(
                        text = stringResource(R.string.verify_sent_to, uiState.destination),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CodeGroup(uiState, onCodeChange, onVerify)
                    MinkaButton(
                        text = uiState.resendSecondsLeft
                            ?.let { stringResource(R.string.verify_resend_in, formatCountdown(it)) }
                            ?: stringResource(R.string.verify_resend),
                        onClick = onResend,
                        style = MinkaButtonStyle.Text,
                        enabled = uiState.canResend,
                        leadingIcon = Icons.Rounded.Refresh,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.verify_support_question),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MinkaButton(
                        text = stringResource(R.string.verify_support_action),
                        onClick = onContactSupport,
                        style = MinkaButtonStyle.Text,
                    )
                }
                MinkaButton(
                    text = stringResource(R.string.verify_submit),
                    onClick = onVerify,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canVerify,
                    size = MinkaButtonSize.Large,
                    loading = uiState.isLoading,
                )
            }
        }
    }
}

/** The 6 boxes and, under them, the attempts left (1.14) or the error (1.15). */
@Composable
private fun CodeGroup(uiState: VerifyCodeUiState, onCodeChange: (String) -> Unit, onVerify: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        MinkaCodeInput(
            value = uiState.code,
            onValueChange = onCodeChange,
            length = CODE_LENGTH,
            label = stringResource(R.string.verify_code_label),
            isError = uiState.codeError != null,
            enabled = !uiState.isLoading,
            onDone = onVerify,
        )
        when (val error = uiState.codeError) {
            null -> Text(
                text = pluralStringResource(R.plurals.verify_attempts, uiState.attemptsLeft, uiState.attemptsLeft),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Row(
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
                    text = when (error) {
                        is CodeError.Wrong ->
                            pluralStringResource(R.plurals.verify_wrong_code, error.attemptsLeft, error.attemptsLeft)
                        CodeError.Exhausted -> stringResource(R.string.verify_exhausted)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun verifyMessageText(message: VerifyMessage): String = when (message) {
    VerifyMessage.Offline -> stringResource(R.string.offline_snackbar)
    VerifyMessage.Unexpected -> stringResource(R.string.sign_in_unexpected)
}

@Composable
private fun VerifyCodePreview(uiState: VerifyCodeUiState) {
    MinkaTheme {
        VerifyCodeContent(
            uiState = uiState,
            onBack = {},
            onCodeChange = {},
            onVerify = {},
            onResend = {},
            onContactSupport = {},
            onMessageShown = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun VerifyCodeEnterPreview() = VerifyCodePreview(
    VerifyCodeUiState(destination = "ana.quispe@gmail.com", attemptsLeft = 5, code = "482", resendSecondsLeft = 28),
)

@Preview
@Composable
private fun VerifyCodeWrongPreview() = VerifyCodePreview(
    VerifyCodeUiState(
        destination = "ana.quispe@gmail.com",
        attemptsLeft = 2,
        code = "482915",
        codeError = CodeError.Wrong(attemptsLeft = 2),
    ),
)
