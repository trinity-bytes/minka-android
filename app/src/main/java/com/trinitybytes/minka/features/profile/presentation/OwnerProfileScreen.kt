package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaErrorState
import com.trinitybytes.minka.core.designsystem.component.MinkaLoadingIndicator
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.presentation.component.ListingCard

private val CardMinWidth = 164.dp

/**
 * The public profile of another user (Figma 11.02) with loading, not found, offline and error states.
 * The rating, the reviews tab, "Raíz Verificada", "Reportar perfil" and "Bloquear" (and the menu that
 * holds them) wait for later phases and are hidden. [onOpenListing] opens a listing of theirs.
 */
@Composable
fun OwnerProfileScreen(
    ownerId: String,
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    viewModel: OwnerProfileViewModel = hiltViewModel<OwnerProfileViewModel, OwnerProfileViewModel.Factory>(
        key = ownerId,
        creationCallback = { it.create(ownerId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OwnerProfileContent(
        state = state,
        onBack = onBack,
        onOpenListing = onOpenListing,
        onRetry = viewModel::refresh,
    )
}

@Composable
internal fun OwnerProfileContent(
    state: OwnerProfileUiState,
    onBack: () -> Unit,
    onOpenListing: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MinkaTopAppBar(title = "", onBack = onBack)
        val profile = state.profile
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                profile != null -> OwnerListings(state, onOpenListing)
                state.loading -> MinkaLoadingIndicator()
                state.notFound -> CenteredError(
                    icon = Icons.Rounded.PersonOff,
                    title = stringResource(R.string.owner_profile_not_found_title),
                    body = stringResource(R.string.owner_profile_not_found_body),
                    actionText = stringResource(R.string.owner_profile_back),
                    onAction = onBack,
                )
                state.offline -> CenteredError(
                    icon = Icons.Rounded.WifiOff,
                    title = stringResource(R.string.offline_title),
                    body = stringResource(R.string.offline_body),
                    actionText = stringResource(R.string.offline_retry),
                    onAction = onRetry,
                )
                else -> CenteredError(
                    icon = Icons.Rounded.ErrorOutline,
                    title = stringResource(R.string.load_error_title),
                    body = stringResource(R.string.load_error_body),
                    actionText = stringResource(R.string.offline_retry),
                    onAction = onRetry,
                )
            }
        }
    }
}

@Composable
private fun OwnerListings(state: OwnerProfileUiState, onOpenListing: (String) -> Unit) {
    val profile = checkNotNull(state.profile)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(CardMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s8),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s12),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "stage") {
            ProfileStage(
                name = profile.owner.name,
                initials = state.initials,
                district = profile.owner.district,
                memberSince = profile.owner.memberSince,
                container = MinkaTheme.colors.skyContainer,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }, contentType = "title") {
            Text(
                text = stringResource(R.string.owner_profile_listings),
                modifier = Modifier.padding(top = MinkaSpacing.s8),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (profile.activeListings.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                Text(
                    text = stringResource(R.string.owner_profile_empty, profile.owner.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(profile.activeListings, key = { it.id }, contentType = { "listing" }) { listing ->
                ListingCard(listing, onClick = { onOpenListing(listing.id) })
            }
        }
    }
}

@Composable
private fun CenteredError(
    icon: ImageVector,
    title: String,
    body: String,
    actionText: String,
    onAction: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(MinkaSpacing.s16), contentAlignment = Alignment.Center) {
        MinkaErrorState(icon = icon, title = title, body = body, actionText = actionText, onAction = onAction)
    }
}
