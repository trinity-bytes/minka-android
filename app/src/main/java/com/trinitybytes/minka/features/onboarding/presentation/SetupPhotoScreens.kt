package com.trinitybytes.minka.features.onboarding.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.trinitybytes.minka.core.designsystem.component.MinkaSticker
import com.trinitybytes.minka.core.designsystem.component.MinkaStickerTone
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma 1.20. "Activar notificaciones" shows the system dialog on Android 13+ (below it, the
 * notifications are on already); whatever the answer, or with "Ahora no", [onDone] continues.
 */
@Composable
fun NotificationsScreen(onDone: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val done by rememberUpdatedState(onDone)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MinkaMessageEffect(
        hostState = snackbar,
        message = uiState.message,
        text = uiState.message?.let { setupMessageText(it) },
        onShown = viewModel::onMessageShown,
    )
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Only emitted on Android 13+, where the permission exists.
                NotificationsEvent.RequestPermission -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                NotificationsEvent.Done -> done()
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        NotificationsContent(onEnable = viewModel::onEnable, onNotNow = viewModel::onNotNow)
        MinkaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}

@Composable
fun NotificationsContent(onEnable: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    PhotoStepLayout(
        stage = NotificationsStage,
        title = stringResource(R.string.setup_notifications_title),
        titleStyle = MaterialTheme.typography.headlineLarge,
        body = stringResource(R.string.setup_notifications_body),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
            MinkaButton(
                text = stringResource(R.string.setup_notifications_enable),
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth(),
                size = MinkaButtonSize.Large,
            )
            MinkaButton(
                text = stringResource(R.string.setup_notifications_not_now),
                onClick = onNotNow,
                modifier = Modifier.fillMaxWidth(),
                style = MinkaButtonStyle.Text,
                size = MinkaButtonSize.Large,
            )
        }
    }
}

/** Figma 1.21. "Explorar Mink'a" completes onboarding; [onGoHome] opens Inicio with nothing behind. */
@Composable
fun AllSetScreen(onGoHome: () -> Unit, viewModel: AllSetViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goHome by rememberUpdatedState(onGoHome)
    val snackbar = remember { SnackbarHostState() }
    MinkaMessageEffect(
        hostState = snackbar,
        message = uiState.message,
        text = uiState.message?.let { setupMessageText(it) },
        onShown = viewModel::onMessageShown,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AllSetEvent.GoHome -> goHome()
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        AllSetContent(uiState = uiState, onExplore = viewModel::onExplore)
        MinkaSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}

@Composable
fun AllSetContent(uiState: AllSetUiState, onExplore: () -> Unit, modifier: Modifier = Modifier) {
    PhotoStepLayout(
        stage = AllSetStage,
        title = if (uiState.firstName.isBlank()) {
            stringResource(R.string.setup_all_set_title_no_name)
        } else {
            stringResource(R.string.setup_all_set_title, uiState.firstName)
        },
        titleStyle = MaterialTheme.typography.displaySmall,
        body = uiState.districtName
            ?.let { stringResource(R.string.setup_all_set_body, it) }
            ?: stringResource(R.string.setup_all_set_body_no_district),
        modifier = modifier,
    ) {
        MinkaButton(
            text = stringResource(R.string.setup_explore),
            onClick = onExplore,
            modifier = Modifier.fillMaxWidth(),
            size = MinkaButtonSize.Large,
            loading = uiState.isFinishing,
        )
    }
}

/** The 1.20/1.21 layout: a photo stage, centered texts and the actions, adapted by [AdaptivePhotoLayout]. */
@Composable
private fun PhotoStepLayout(
    stage: PhotoStage,
    title: String,
    titleStyle: TextStyle,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AdaptivePhotoLayout(
            photo = { photoModifier -> PhotoStageBox(stage, photoModifier) },
            modifier = Modifier
                .safeDrawingPadding()
                .padding(MinkaSpacing.s16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            actions()
        }
    }
}

/** Bento stage with the blob photo running off its edges, the icon disc and the sticker (as in 1.02–1.04). */
@Composable
private fun PhotoStageBox(stage: PhotoStage, modifier: Modifier = Modifier) {
    val colors = stage.colors()
    Box(modifier.clip(MinkaShapes.bento).background(colors.stage)) {
        Image(
            painter = painterResource(stage.photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .overflowing(x = 8.dp, y = stage.photoTop, extraWidth = 60.dp, extraHeight = stage.photoOverflow)
                .clip(stage.blob),
        )
        Box(
            modifier = Modifier
                .padding(MinkaSpacing.s20)
                .size(64.dp)
                .background(colors.stage, CircleShape)
                .padding(MinkaSpacing.s4)
                .background(colors.disc, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(stage.icon, contentDescription = null, tint = colors.onDisc, modifier = Modifier.size(32.dp))
        }
        MinkaSticker(
            text = stringResource(stage.sticker),
            tone = stage.stickerTone,
            rotation = stage.stickerRotation,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = MinkaSpacing.s20, bottom = stage.stickerBottom),
        )
    }
}

private enum class StageAccent { Sky, Coral }

private data class StageColors(val stage: Color, val disc: Color, val onDisc: Color)

@Composable
private fun PhotoStage.colors(): StageColors {
    val minka = MinkaTheme.colors
    return when (accent) {
        StageAccent.Sky -> StageColors(minka.skyContainer, minka.sky, minka.onSky)
        StageAccent.Coral -> StageColors(minka.coralContainer, minka.coral, minka.onCoral)
    }
}

private data class PhotoStage(
    @DrawableRes val photo: Int,
    val blob: Shape,
    val photoTop: Dp,
    /** How far the photo runs past the stage bottom (Figma photo height − stage height + top). */
    val photoOverflow: Dp,
    val icon: ImageVector,
    val accent: StageAccent,
    @StringRes val sticker: Int,
    val stickerTone: MinkaStickerTone,
    val stickerRotation: Float,
    val stickerBottom: Dp,
)

private val NotificationsStage = PhotoStage(
    photo = R.drawable.onboarding_notifications,
    blob = MinkaExpressiveShapes.BlobA,
    photoTop = 31.dp,
    photoOverflow = 94.dp,
    icon = Icons.Rounded.NotificationsActive,
    accent = StageAccent.Sky,
    sticker = R.string.setup_notifications_sticker,
    stickerTone = MinkaStickerTone.Ink,
    stickerRotation = 4f,
    stickerBottom = 32.dp,
)

private val AllSetStage = PhotoStage(
    photo = R.drawable.onboarding_all_set,
    blob = MinkaExpressiveShapes.BlobB,
    photoTop = 37.dp,
    photoOverflow = 112.dp,
    icon = Icons.Rounded.Check,
    accent = StageAccent.Coral,
    sticker = R.string.setup_all_set_sticker,
    stickerTone = MinkaStickerTone.Paper,
    stickerRotation = -5f,
    stickerBottom = 44.dp,
)

@Preview
@Composable
private fun NotificationsPreview() {
    MinkaTheme { NotificationsContent(onEnable = {}, onNotNow = {}) }
}

@Preview
@Composable
private fun AllSetPreview() {
    MinkaTheme { AllSetContent(AllSetUiState(firstName = "Ana", districtName = "Miraflores"), onExplore = {}) }
}
