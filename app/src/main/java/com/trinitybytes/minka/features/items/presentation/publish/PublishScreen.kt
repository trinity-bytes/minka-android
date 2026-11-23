package com.trinitybytes.minka.features.items.presentation.publish

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.FormMaxWidth
import com.trinitybytes.minka.core.designsystem.component.MinkaAdaptiveBox
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadErrorScreen
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadingIndicator
import com.trinitybytes.minka.core.designsystem.component.MinkaMessageEffect
import com.trinitybytes.minka.core.designsystem.component.MinkaSnackbarHost
import com.trinitybytes.minka.core.designsystem.component.ObserveAsEvents
import com.trinitybytes.minka.core.designsystem.component.isExpandedWidth
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.DraftError
import com.trinitybytes.minka.features.items.domain.MAX_PHOTOS
import com.trinitybytes.minka.navigation.PublishRoute
import java.io.File
import kotlinx.coroutines.launch

/**
 * Publicar, full screen (Figma 5.01–5.11): three steps with "Guardar borrador" on each, the 5.09
 * loading state and the 5.10 confirmation. [route] carries the draft to resume, if any;
 * [onViewListing] opens the published listing from 5.10.
 */
@Composable
fun PublishScreen(
    route: PublishRoute,
    onClose: () -> Unit,
    onViewListing: (String) -> Unit,
    viewModel: PublishViewModel = hiltViewModel<PublishViewModel, PublishViewModel.Factory>(
        creationCallback = { it.create(route) },
    ),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(onClose)
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            PublishEvent.Close -> leave()
        }
    }

    val photoImportFailed = stringResource(R.string.publish_message_photo_failed)
    val noCamera = stringResource(R.string.publish_message_no_camera)
    val maxItems = (MAX_PHOTOS - state.draft.photos.size).coerceAtLeast(2)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
        scope.launch {
            val imported = uris.mapNotNull { PublishPhotoFiles.import(context, it) }
            if (imported.size < uris.size) snackbarHostState.showSnackbar(photoImportFailed)
            if (imported.isNotEmpty()) viewModel.onPhotosPicked(imported)
        }
    }
    // The camera app may outlive this process, so the target file is kept across recreation.
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val file = cameraFile?.let(::File)
        cameraFile = null
        if (file == null) return@rememberLauncherForActivityResult
        val photo = if (taken) PublishPhotoFiles.cameraResult(file) else null
        if (photo != null) viewModel.onPhotosPicked(listOf(photo)) else PublishPhotoFiles.discard(file)
    }
    val shareChooser = stringResource(R.string.publish_share_chooser)
    val actions = publishActionsOf(
        viewModel = viewModel,
        onLeave = onClose,
        onTakePhoto = {
            viewModel.dismissOverlay()
            val (file, uri) = PublishPhotoFiles.newCameraTarget(context)
            cameraFile = file.path
            try {
                camera.launch(uri)
            } catch (_: ActivityNotFoundException) {
                cameraFile = null
                PublishPhotoFiles.discard(file)
                scope.launch { snackbarHostState.showSnackbar(noCamera) }
            }
        },
        onPickFromGallery = {
            viewModel.dismissOverlay()
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onViewListing = onViewListing,
        onShare = {
            val title = state.published?.title ?: return@publishActionsOf
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.publish_share_text, title))
            try {
                context.startActivity(Intent.createChooser(send, shareChooser))
            } catch (_: ActivityNotFoundException) {
                // No app can take the text; nothing else to offer.
            }
        },
    )
    PublishContent(state = state, actions = actions, snackbarHostState = snackbarHostState)
}

/** Every state of the flow, driven by [state]; the platform pieces (camera, picker, share) arrive in [actions]. */
@Composable
fun PublishContent(
    state: PublishUiState,
    actions: PublishActions,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    MinkaMessageEffect(
        hostState = snackbarHostState,
        message = state.message,
        text = state.message?.let { messageText(it) },
        onShown = actions.onMessageShown,
    )
    BackHandler(enabled = state.phase != PublishPhase.Loading && state.phase != PublishPhase.LoadFailed) {
        when (state.phase) {
            PublishPhase.Editing -> actions.onBack()
            PublishPhase.Published -> actions.onLeave()
            else -> Unit
        }
    }
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            PublishPhase.Loading -> LoadingPage(actions)
            PublishPhase.LoadFailed -> MinkaLoadErrorScreen(onBack = actions.onLeave, onRetry = actions.onRetryLoad)
            PublishPhase.Editing -> EditingPage(state, actions, snackbarHostState)
            PublishPhase.Publishing -> CenteredPage(snackbarHostState) {
                PublishingPage(photoCount = state.draft.photos.size)
            }
            PublishPhase.Published -> CenteredPage(snackbarHostState) {
                state.published?.let { PublishedPage(it, actions) }
            }
        }
        if (state.phase == PublishPhase.Editing) PublishOverlays(state, actions)
    }
}

@Composable
private fun messageText(message: PublishMessage): String = stringResource(
    when (message) {
        PublishMessage.DraftSaved -> R.string.publish_message_draft_saved
        PublishMessage.Offline -> R.string.offline_snackbar
        PublishMessage.PublishFailed -> R.string.publish_message_publish_failed
        PublishMessage.Unexpected -> R.string.publish_message_unexpected
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingPage(actions: PublishActions) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { PublishTopBar(closeIcon = true, onClick = actions.onLeave) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            MinkaLoadingIndicator()
        }
    }
}

/** 5.09 and 5.10 have no top bar: one centered column of at most [FormMaxWidth]. */
@Composable
private fun CenteredPage(snackbarHostState: SnackbarHostState, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { MinkaSnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = FormMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(MinkaSpacing.s16),
            ) { content() }
        }
    }
}

@Composable
private fun EditingPage(state: PublishUiState, actions: PublishActions, snackbarHostState: SnackbarHostState) {
    val closeIcon = state.page == PublishPage.Basics && !state.returnToReview
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            PublishTopBar(closeIcon = closeIcon, onClick = if (closeIcon) actions.onClose else actions.onBack)
        },
        snackbarHost = { MinkaSnackbarHost(snackbarHostState) },
    ) { padding ->
        MinkaAdaptiveBox(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            val twoPanes = isExpandedWidth && (state.page == PublishPage.Basics || state.page == PublishPage.Review)
            if (twoPanes) {
                TwoPaneForm(state, actions)
            } else {
                SinglePaneForm(state, actions)
            }
        }
    }
}

/** The close icon on the first step and the back arrow after it (audit finding 19). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishTopBar(closeIcon: Boolean, onClick: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (closeIcon) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(if (closeIcon) R.string.nav_close else R.string.action_back),
                )
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = background,
            scrolledContainerColor = background,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun primaryText(page: PublishPage): String =
    stringResource(if (page == PublishPage.Review) R.string.publish_publish else R.string.publish_continue)

/** Compact and medium windows, and the expanded pages that need only one column (Figma 5.06, 5.07). */
@Composable
private fun SinglePaneForm(state: PublishUiState, actions: PublishActions) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = FormMaxWidth).fillMaxWidth().fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = MinkaSpacing.s16, end = MinkaSpacing.s16, top = MinkaSpacing.s8),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                when (state.page) {
                    PublishPage.Basics -> BasicsPage(state, actions)
                    PublishPage.Wants -> WantsPage(state, actions)
                    PublishPage.Location -> LocationPage(state, actions)
                    PublishPage.Review -> ReviewPage(state.draft, actions)
                }
            }
            PublishActionBar(
                primaryText = primaryText(state.page),
                onPrimary = actions.onContinue,
                onSaveDraft = actions.onSaveDraft,
                isSaving = state.isSaving,
                modifier = Modifier.padding(MinkaSpacing.s16),
            )
        }
    }
}

/** Step 1 on a narrow window: photo strip, helper text and the fields (Figma 5.01, 5.03, 5.05). */
@Composable
private fun BasicsPage(state: PublishUiState, actions: PublishActions) {
    StepProgress(PublishPage.Basics)
    PageTitle(stringResource(R.string.publish_basics_title))
    PhotoStrip(
        photos = state.draft.photos,
        canAdd = state.canAddPhotos,
        error = DraftErrorPhoto in state.errors,
        actions = actions,
    )
    PhotoHint(hasPhotos = state.draft.photos.isNotEmpty(), error = DraftErrorPhoto in state.errors)
    BasicsFields(state.draft, state.errors, actions)
    RequiredNote()
}

/**
 * Expanded windows (Figma "Ventanas ≥840dp"): the photos or the preview on the left and the form,
 * kept at a readable width, on the right with its actions pinned below.
 */
@Composable
private fun TwoPaneForm(state: PublishUiState, actions: PublishActions) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = MinkaSpacing.s24),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s24, Alignment.CenterHorizontally),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = PaneMaxWidth)
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(top = MinkaSpacing.s8, bottom = MinkaSpacing.s16),
            verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            if (state.page == PublishPage.Basics) {
                PhotoGrid(
                    photos = state.draft.photos,
                    canAdd = state.canAddPhotos,
                    error = DraftErrorPhoto in state.errors,
                    actions = actions,
                )
                PhotoHint(hasPhotos = state.draft.photos.isNotEmpty(), error = DraftErrorPhoto in state.errors)
            } else {
                PreviewPane(state.draft)
            }
        }
        Column(modifier = Modifier.widthIn(max = FormMaxWidth).weight(1f, fill = false)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = MinkaSpacing.s8),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                if (state.page == PublishPage.Basics) {
                    StepProgress(PublishPage.Basics)
                    PageTitle(stringResource(R.string.publish_basics_title))
                    BasicsFields(state.draft, state.errors, actions)
                    RequiredNote()
                } else {
                    ReviewPage(state.draft, actions, showPreview = false)
                }
            }
            PublishActionBar(
                primaryText = primaryText(state.page),
                onPrimary = actions.onContinue,
                onSaveDraft = actions.onSaveDraft,
                isSaving = state.isSaving,
                modifier = Modifier.padding(vertical = MinkaSpacing.s16),
            )
        }
    }
}

private val PaneMaxWidth = 420.dp
private val DraftErrorPhoto = DraftError.PHOTO_REQUIRED
