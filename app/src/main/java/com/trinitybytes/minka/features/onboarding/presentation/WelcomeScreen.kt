package com.trinitybytes.minka.features.onboarding.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.ExpandedWidthBreakpoint
import com.trinitybytes.minka.core.designsystem.component.MinkaAdaptiveBox
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonSize
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaExpressiveShapes
import com.trinitybytes.minka.core.designsystem.component.MinkaGoogleButton
import com.trinitybytes.minka.core.designsystem.component.isExpandedWidth
import com.trinitybytes.minka.core.designsystem.component.MinkaSticker
import com.trinitybytes.minka.core.designsystem.component.MinkaStickerTone
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Figma 1.02–1.04. The access group on the last step leads to registration and sign-in. */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WelcomeContent(
        uiState = uiState,
        onNext = viewModel::next,
        onSkip = viewModel::skip,
        onStepShown = viewModel::onStepShown,
        onCreateAccount = onCreateAccount,
        onSignIn = onSignIn,
        onContinueWithGoogle = onContinueWithGoogle,
    )
}

@Composable
fun WelcomeContent(
    uiState: WelcomeUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onStepShown: (Int) -> Unit,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = uiState.step) { WelcomeUiState.STEP_COUNT }
    LaunchedEffect(uiState.step) {
        if (pagerState.currentPage != uiState.step) pagerState.animateScrollToPage(uiState.step)
    }
    val stepShown by rememberUpdatedState(onStepShown)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { stepShown(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(top = MinkaSpacing.s16),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopActionsHeight)
                .padding(horizontal = MinkaSpacing.s16),
            horizontalArrangement = Arrangement.End,
        ) {
            // Skipping only makes sense before the access step; the slot keeps the layout steady.
            if (!uiState.isLastStep) {
                MinkaButton(text = stringResource(R.string.welcome_skip), onClick = onSkip, style = MinkaButtonStyle.Text)
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            WelcomePage(
                step = page,
                onNext = onNext,
                onCreateAccount = onCreateAccount,
                onSignIn = onSignIn,
                onContinueWithGoogle = onContinueWithGoogle,
            )
        }
    }
}

@Composable
private fun WelcomePage(
    step: Int,
    onNext: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    onContinueWithGoogle: () -> Unit,
) {
    val content = WelcomeSteps[step]
    val isLastStep = step == WelcomeUiState.LAST_STEP
    AdaptivePhotoLayout(
        photo = { photoModifier -> PhotoStage(content, photoModifier) },
        modifier = Modifier.fillMaxSize().padding(MinkaSpacing.s16),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12)) {
            Text(
                text = stringResource(content.title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(content.body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isLastStep) {
            PageIndicator(step, Modifier.align(Alignment.CenterHorizontally))
            Column(verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
                MinkaButton(
                    text = stringResource(R.string.welcome_create_account),
                    onClick = onCreateAccount,
                    modifier = Modifier.fillMaxWidth(),
                    size = MinkaButtonSize.Large,
                )
                MinkaButton(
                    text = stringResource(R.string.welcome_sign_in),
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    style = MinkaButtonStyle.Outlined,
                    size = MinkaButtonSize.Large,
                )
                // Design A4: the official G mark, as Google's sign-in branding requires.
                MinkaGoogleButton(
                    text = stringResource(R.string.welcome_google),
                    onClick = onContinueWithGoogle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageIndicator(step)
                MinkaButton(text = stringResource(R.string.welcome_next), onClick = onNext, size = MinkaButtonSize.Large)
            }
        }
    }
}

/**
 * A photo stage with texts and actions (Figma 1.02–1.04, 1.20, 1.21; design A2, A5).
 *
 * From [ExpandedWidthBreakpoint] the photo fills the start pane and the texts and actions sit
 * bottom-aligned in the end pane, scrolling if they do not fit. Below it they stack: the texts and
 * actions take what they need, up to all but [MinPhotoHeight], and scroll past that, so the photo
 * shrinks first and the actions stay reachable in phone landscape and at 200 % font scale.
 */
@Composable
internal fun AdaptivePhotoLayout(
    photo: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    MinkaAdaptiveBox(modifier) {
        val scroll = rememberScrollState()
        val gap = MinkaSpacing.s16
        val bounded = constraints.hasBoundedHeight
        val paneHeight = if (bounded) maxHeight else 0.dp
        val textMaxHeight = if (bounded) (maxHeight - MinPhotoHeight - gap).coerceAtLeast(0.dp) else Dp.Infinity
        if (isExpandedWidth) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s24)) {
                photo(Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scroll)
                        .heightIn(min = paneHeight),
                    verticalArrangement = Arrangement.spacedBy(gap, Alignment.Bottom),
                    horizontalAlignment = horizontalAlignment,
                    content = content,
                )
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                photo(Modifier.weight(1f).fillMaxWidth())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = textMaxHeight)
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    horizontalAlignment = horizontalAlignment,
                    content = content,
                )
            }
        }
    }
}

/** The least the photo stage shrinks to before the texts and actions start scrolling (design A5). */
private val MinPhotoHeight = 96.dp

/** The bento stage: a blob photo that runs off its right and bottom edges, the icon disc and a sticker. */
@Composable
private fun PhotoStage(content: WelcomeStepContent, modifier: Modifier = Modifier) {
    val accent = content.accent.colors()
    Box(modifier.clip(MinkaShapes.bento).background(accent.stage)) {
        Image(
            painter = painterResource(content.photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .overflowing(x = 8.dp, y = content.photoTop, extraWidth = 60.dp, extraHeight = content.photoOverflow)
                .clip(content.blob),
        )
        Box(
            modifier = Modifier
                .padding(MinkaSpacing.s20)
                .size(64.dp)
                .background(accent.stage, CircleShape)
                .padding(MinkaSpacing.s4)
                .background(accent.disc, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(content.icon, contentDescription = null, tint = accent.onDisc, modifier = Modifier.size(32.dp))
        }
        MinkaSticker(
            text = stringResource(content.sticker),
            tone = content.stickerTone,
            rotation = content.stickerRotation,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = MinkaSpacing.s20, bottom = content.stickerBottom),
        )
    }
}

/**
 * Lays the photo out at ([x], [y]) inside the stage, larger than the stage by [extraWidth] ×
 * [extraHeight], so the stage clips it as in Figma.
 */
internal fun Modifier.overflowing(x: Dp, y: Dp, extraWidth: Dp, extraHeight: Dp) = layout { measurable, constraints ->
    val width = constraints.maxWidth + extraWidth.roundToPx()
    val height = constraints.maxHeight + extraHeight.roundToPx() - y.roundToPx()
    val placeable = measurable.measure(Constraints.fixed(width, height.coerceAtLeast(0)))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x.roundToPx(), y.roundToPx()) }
}

@Composable
private fun PageIndicator(step: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.welcome_step_indicator, step + 1, WelcomeUiState.STEP_COUNT)
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
    ) {
        repeat(WelcomeUiState.STEP_COUNT) { index ->
            val active = index == step
            Box(
                Modifier
                    .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.extraSmall,
                    ),
            )
        }
    }
}

/** Each step has its accent: lavender, amber, mint (Figma 1.02 note). */
private enum class WelcomeAccent { Lavender, Amber, Mint }

private data class AccentColors(val stage: Color, val disc: Color, val onDisc: Color)

@Composable
@ReadOnlyComposable
private fun WelcomeAccent.colors(): AccentColors {
    val scheme = MaterialTheme.colorScheme
    val minka = MinkaTheme.colors
    return when (this) {
        WelcomeAccent.Lavender -> AccentColors(minka.lavenderContainer, scheme.secondaryContainer, scheme.onSecondaryContainer)
        WelcomeAccent.Amber -> AccentColors(scheme.tertiaryContainer, minka.amber, minka.onAmber)
        WelcomeAccent.Mint -> AccentColors(
            stage = minka.mint,
            disc = minka.brand,
            onDisc = minka.onBrand,
        )
    }
}

private data class WelcomeStepContent(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val sticker: Int,
    val stickerTone: MinkaStickerTone,
    val stickerRotation: Float,
    val stickerBottom: Dp,
    val icon: ImageVector,
    @DrawableRes val photo: Int,
    val blob: Shape,
    val photoTop: Dp,
    /** How far the photo runs past the stage bottom; the last stage is shorter (Figma 1.04 note). */
    val photoOverflow: Dp,
    val accent: WelcomeAccent,
)

private val WelcomeSteps = listOf(
    WelcomeStepContent(
        title = R.string.welcome_1_title,
        body = R.string.welcome_1_body,
        sticker = R.string.welcome_1_sticker,
        stickerTone = MinkaStickerTone.Paper,
        stickerRotation = -4f,
        stickerBottom = 28.dp,
        icon = Icons.Rounded.SwapHoriz,
        photo = R.drawable.onboarding_welcome_1,
        blob = MinkaExpressiveShapes.BlobA,
        photoTop = 32.dp,
        photoOverflow = 111.dp,
        accent = WelcomeAccent.Lavender,
    ),
    WelcomeStepContent(
        title = R.string.welcome_2_title,
        body = R.string.welcome_2_body,
        sticker = R.string.welcome_2_sticker,
        stickerTone = MinkaStickerTone.Ink,
        stickerRotation = 5f,
        stickerBottom = 12.dp,
        icon = Icons.Rounded.LocationOn,
        photo = R.drawable.onboarding_welcome_2,
        blob = MinkaExpressiveShapes.BlobB,
        photoTop = 32.dp,
        photoOverflow = 111.dp,
        accent = WelcomeAccent.Amber,
    ),
    WelcomeStepContent(
        title = R.string.welcome_3_title,
        body = R.string.welcome_3_body,
        sticker = R.string.welcome_3_sticker,
        stickerTone = MinkaStickerTone.Paper,
        stickerRotation = -5f,
        stickerBottom = 28.dp,
        icon = Icons.Rounded.Eco,
        photo = R.drawable.onboarding_welcome_3,
        blob = MinkaExpressiveShapes.BlobC,
        photoTop = 23.dp,
        photoOverflow = 84.dp,
        accent = WelcomeAccent.Mint,
    ),
)

private val TopActionsHeight = 40.dp

@Preview
@Composable
private fun WelcomeFirstStepPreview() {
    MinkaTheme { WelcomeContent(WelcomeUiState(step = 0), {}, {}, {}, {}, {}, {}) }
}

@Preview
@Composable
private fun WelcomeLastStepPreview() {
    MinkaTheme { WelcomeContent(WelcomeUiState(step = WelcomeUiState.LAST_STEP), {}, {}, {}, {}, {}, {}) }
}
