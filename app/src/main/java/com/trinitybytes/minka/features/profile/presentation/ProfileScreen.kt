package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaListItem
import com.trinitybytes.minka.core.designsystem.component.MinkaTopAppBar
import com.trinitybytes.minka.core.designsystem.component.formColumnWidth
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * The signed-in user's own profile (Figma 11.01, basic). Rows carry no chevrons (audit P5). Settings,
 * Mis intercambios, Reseñas, Eco-Puntos and the impact dashboard belong to later phases and are hidden.
 * The member-since line is omitted: the session does not know the account's creation date.
 */
@Composable
fun ProfileScreen(
    onOpenMyListings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileContent(state = state, onOpenMyListings = onOpenMyListings)
}

@Composable
internal fun ProfileContent(
    state: ProfileUiState,
    onOpenMyListings: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        MinkaTopAppBar(title = stringResource(R.string.profile_title))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .formColumnWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s8),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                ProfileStage(
                    name = state.name,
                    initials = state.initials,
                    district = state.district,
                    memberSince = null,
                    container = MinkaTheme.colors.mint,
                )
                MinkaListItem(
                    headline = stringResource(R.string.profile_my_listings),
                    leadingIcon = Icons.Rounded.Inventory2,
                    onClick = onOpenMyListings,
                    modifier = Modifier.clip(MaterialTheme.shapes.large),
                )
            }
        }
    }
}
