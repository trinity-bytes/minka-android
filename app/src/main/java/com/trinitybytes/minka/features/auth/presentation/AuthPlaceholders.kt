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

// TEMPORARY: placeholder so registration continues into the onboarding end to end.
// DistrictPlaceholderScreen is replaced by T7 (Figma 1.16 and on); delete this file then.

@Composable
fun DistrictPlaceholderScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.district_placeholder), modifier = modifier)
}

@Composable
private fun PlaceholderScreen(title: String, modifier: Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
