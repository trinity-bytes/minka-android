package com.trinitybytes.minka.features.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.ObserveAsEvents
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadErrorScreen
import com.trinitybytes.minka.core.designsystem.component.MinkaMessageEffect
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaOfflineScreen
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.formColumnWidth
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.onboarding.domain.Interest

/** Figma 1.19. [onDone] continues with notifications, after saving or skipping. */
@Composable
fun InterestsScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: InterestsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val done by rememberUpdatedState(onDone)
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            InterestsEvent.Done -> done()
        }
    }
    InterestsContent(
        uiState = uiState,
        onBack = onBack,
        onInterestToggled = viewModel::onInterestToggled,
        onSkip = viewModel::onSkip,
        onContinue = viewModel::onContinue,
        onMessageShown = viewModel::onMessageShown,
        onRetry = viewModel::retry,
    )
}

@Composable
fun InterestsContent(
    uiState: InterestsUiState,
    onBack: () -> Unit,
    onInterestToggled: (String) -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A list that could not load leaves nothing to show: full-screen error with "Reintentar" (1.22 offline).
    when (uiState.loadError) {
        LoadError.Offline -> {
            MinkaOfflineScreen(onBack = onBack, onRetry = onRetry, modifier = modifier, retrying = uiState.isLoading)
            return
        }
        LoadError.Unexpected -> {
            MinkaLoadErrorScreen(onBack = onBack, onRetry = onRetry, modifier = modifier, retrying = uiState.isLoading)
            return
        }
        null -> Unit
    }
    val snackbarHostState = remember { SnackbarHostState() }
    // 1.22: a failed save keeps the chips and the snackbar offers "Reintentar".
    MinkaMessageEffect(
        hostState = snackbarHostState,
        message = uiState.message,
        text = uiState.message?.let { setupMessageText(it) },
        onShown = onMessageShown,
        actionLabel = stringResource(R.string.offline_retry).takeIf { uiState.message == SetupMessage.Offline },
        onAction = onRetry,
    )
    // Design B5: the step keeps a centered column of at most 480 dp.
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Scaffold(
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
                    SetupStepHeader(
                        progress = 1f,
                        step = stringResource(R.string.setup_interests_step),
                        title = stringResource(R.string.setup_interests_title),
                    )
                    Text(
                        text = stringResource(R.string.setup_interests_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                    ) {
                        uiState.interests.forEach { interest ->
                            MinkaFilterChip(
                                label = interest.name,
                                selected = interest.id in uiState.selectedIds,
                                onClick = { onInterestToggled(interest.id) },
                                enabled = !uiState.isSaving,
                            )
                        }
                    }
                }
                val count = uiState.selectedIds.size
                if (count > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.setup_interests_selected, count, count),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinkaButton(
                        text = stringResource(R.string.setup_skip),
                        onClick = onSkip,
                        style = MinkaButtonStyle.Text,
                        enabled = !uiState.isSaving,
                        size = MinkaButtonSize.Large,
                    )
                    MinkaButton(
                        text = stringResource(R.string.setup_continue),
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        enabled = uiState.canContinue,
                        size = MinkaButtonSize.Large,
                        loading = uiState.isSaving,
                    )
                }
            }
        }
    }
}

private val PreviewInterests = listOf(
    "Hogar", "Ropa", "Libros", "Tecnología", "Deportes", "Niños",
    "Herramientas", "Plantas", "Servicios", "Arte", "Música", "Cocina",
).map { Interest(it.lowercase(), it) }

@Preview
@Composable
private fun InterestsPreview() {
    MinkaTheme {
        InterestsContent(
            uiState = InterestsUiState(
                interests = PreviewInterests,
                selectedIds = setOf("libros", "herramientas", "plantas"),
                isLoading = false,
            ),
            onBack = {},
            onInterestToggled = {},
            onSkip = {},
            onContinue = {},
            onMessageShown = {},
            onRetry = {},
        )
    }
}
