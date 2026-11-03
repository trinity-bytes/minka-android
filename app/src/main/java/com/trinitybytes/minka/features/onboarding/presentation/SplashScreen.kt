package com.trinitybytes.minka.features.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaExpressiveShapes
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadingIndicator
import com.trinitybytes.minka.core.designsystem.component.MinkaLogo
import com.trinitybytes.minka.core.designsystem.component.MinkaLogoType
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Figma 1.01: restores the session, then hands over to [onNavigate]. */
@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigate by rememberUpdatedState(onNavigate)
    LaunchedEffect(uiState) {
        (uiState as? SplashUiState.Ready)?.let { navigate(it.destination) }
    }
    SplashContent(isRestoring = uiState == SplashUiState.Restoring)
}

@Composable
fun SplashContent(isRestoring: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(MinkaSpacing.s16),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(CookieSize)
                .background(MinkaTheme.colors.brand, MinkaExpressiveShapes.Cookie12),
        ) {
            // Figma places the mark at (68, 61.25) inside the 220 dp cookie.
            MinkaLogo(
                modifier = Modifier.offset(x = 68.dp, y = 61.25.dp).height(97.5.dp),
                type = MinkaLogoType.Logomark,
                leafColor = MinkaTheme.colors.onBrand,
            )
        }
        MinkaLogo(Modifier.width(176.dp).height(51.dp))
        Spacer(Modifier.weight(1f))
        val loadingDescription = stringResource(R.string.splash_loading)
        Box(Modifier.size(48.dp)) {
            if (isRestoring) {
                MinkaLoadingIndicator(Modifier.semantics { contentDescription = loadingDescription })
            }
        }
    }
}

private val CookieSize = 220.dp

@PreviewLightDark
@Composable
private fun SplashPreview() {
    MinkaTheme { SplashContent(isRestoring = true) }
}
