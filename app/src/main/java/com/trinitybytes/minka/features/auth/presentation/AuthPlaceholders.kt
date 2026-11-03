package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trinitybytes.minka.R

// TEMPORARY: placeholders so the welcome access group navigates end to end.
// SignInPlaceholderScreen is replaced by T4 (Figma 1.05–1.09) and RegisterPlaceholderScreen by
// T6 (Figma 1.12–1.15); delete this file then.

@Composable
fun SignInPlaceholderScreen(withGoogle: Boolean, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.sign_in_placeholder) + if (withGoogle) " · Google" else "",
        modifier = modifier,
    )
}

@Composable
fun RegisterPlaceholderScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.register_placeholder), modifier = modifier)
}

@Composable
private fun PlaceholderScreen(title: String, modifier: Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
