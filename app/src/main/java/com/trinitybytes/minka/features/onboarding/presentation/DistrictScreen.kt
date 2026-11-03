package com.trinitybytes.minka.features.onboarding.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaRadioRow
import com.trinitybytes.minka.core.designsystem.component.MinkaSearchField
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.MinkaWavyProgress
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.onboarding.domain.District

/**
 * Figma 1.16–1.18. "Usar mi ubicación" asks the system for the approximate location (1.17); a
 * denial shows the snackbar with "Ajustes" (1.18) and the list stays usable. [onChosen] continues
 * with the interests once the district is saved. [onBack] is null when nothing is behind.
 */
@Composable
fun DistrictScreen(
    onBack: (() -> Unit)?,
    onChosen: () -> Unit,
    viewModel: DistrictViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chosen by rememberUpdatedState(onChosen)
    val context = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onLocationPermissionResult(granted)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // 1.17 "Basta la aproximada": fine location is never requested.
                DistrictEvent.RequestLocationPermission -> locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                DistrictEvent.Chosen -> chosen()
            }
        }
    }
    DistrictContent(
        uiState = uiState,
        onBack = onBack,
        onUseMyLocation = viewModel::onUseMyLocation,
        onQueryChange = viewModel::onQueryChange,
        onDistrictSelected = viewModel::onDistrictSelected,
        onContinue = viewModel::onContinue,
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        onMessageShown = viewModel::onMessageShown,
    )
}

/** One composable for 1.16, 1.17 (under the system dialog) and 1.18, driven by [uiState]. */
@Composable
fun DistrictContent(
    uiState: DistrictUiState,
    onBack: (() -> Unit)?,
    onUseMyLocation: () -> Unit,
    onQueryChange: (String) -> Unit,
    onDistrictSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { setupMessageText(it) }
    val settingsLabel = stringResource(R.string.setup_settings)
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            val denied = uiState.message == SetupMessage.LocationDenied
            val result = snackbarHostState.showSnackbar(
                message = messageText,
                actionLabel = settingsLabel.takeIf { denied },
                duration = if (denied) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onOpenSettings()
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
                SetupStepHeader(
                    progress = 2f / 3,
                    step = stringResource(R.string.setup_district_step),
                    title = stringResource(R.string.setup_district_title),
                )
                Text(
                    text = stringResource(R.string.setup_district_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MinkaButton(
                    text = stringResource(R.string.setup_use_location),
                    onClick = onUseMyLocation,
                    modifier = Modifier.fillMaxWidth(),
                    style = MinkaButtonStyle.Outlined,
                    enabled = !uiState.isSaving,
                    leadingIcon = Icons.Rounded.MyLocation,
                    size = MinkaButtonSize.Large,
                    loading = uiState.isLocating,
                )
                MinkaSearchField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.setup_district_search),
                    enabled = !uiState.isSaving,
                )
                DistrictList(uiState.visibleDistricts, uiState.selectedId, !uiState.isSaving, onDistrictSelected)
            }
            MinkaButton(
                text = stringResource(R.string.setup_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canContinue,
                size = MinkaButtonSize.Large,
                loading = uiState.isSaving,
            )
        }
    }
}

@Composable
private fun DistrictList(
    districts: List<District>,
    selectedId: String?,
    enabled: Boolean,
    onDistrictSelected: (String) -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        districts.forEach { district ->
            MinkaRadioRow(
                selected = district.id == selectedId,
                onClick = { onDistrictSelected(district.id) },
                label = district.name,
                enabled = enabled,
            )
        }
    }
}

/** Wavy progress, "Paso N de 3 · …" and the headline, as in the registration step (1.12). */
@Composable
internal fun SetupStepHeader(progress: Float, step: String, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
        MinkaWavyProgress(progress = progress)
        Text(text = step, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(text = title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
internal fun setupMessageText(message: SetupMessage): String = when (message) {
    SetupMessage.LocationDenied -> stringResource(R.string.setup_location_denied)
    SetupMessage.Offline -> stringResource(R.string.sign_in_offline)
    SetupMessage.Unexpected -> stringResource(R.string.sign_in_unexpected)
}

private val PreviewDistricts = listOf(
    District("miraflores", "Miraflores"),
    District("san-isidro", "San Isidro"),
    District("barranco", "Barranco"),
    District("santiago-de-surco", "Santiago de Surco"),
    District("jesus-maria", "Jesús María"),
)

@Composable
private fun DistrictPreview(uiState: DistrictUiState) {
    MinkaTheme {
        DistrictContent(
            uiState = uiState,
            onBack = {},
            onUseMyLocation = {},
            onQueryChange = {},
            onDistrictSelected = {},
            onContinue = {},
            onOpenSettings = {},
            onMessageShown = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DistrictEmptyPreview() = DistrictPreview(DistrictUiState(districts = PreviewDistricts, isLoading = false))

@PreviewLightDark
@Composable
private fun DistrictChosenPreview() =
    DistrictPreview(DistrictUiState(districts = PreviewDistricts, selectedId = "miraflores", isLoading = false))
