package com.trinitybytes.minka.features.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaFilterChip
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaOfflineScreen
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
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
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                InterestsEvent.Done -> done()
            }
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
    // 1.22: no connection replaces the step; the typed values stay in [uiState] for the retry.
    if (uiState.isOffline) {
        MinkaOfflineScreen(onBack = onBack, onRetry = onRetry, modifier = modifier, retrying = uiState.isLoading || uiState.isSaving)
        return
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { setupMessageText(it) }
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

private val PreviewInterests = listOf(
    "Hogar", "Ropa", "Libros", "Tecnología", "Deportes", "Niños",
    "Herramientas", "Plantas", "Servicios", "Arte", "Música", "Cocina",
).map { Interest(it.lowercase(), it) }

@PreviewLightDark
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
