package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaElevation
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.core.designsystem.theme.minkaShadow

/** Snackbar host that renders every message as a [MinkaSnackbar]. Put it in the Scaffold slot. */
@Composable
fun MinkaSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { MinkaSnackbar(it) }
}

/** Figma Snackbar (`299:99`): inverse surface, radius medium and the lg shadow. */
@Composable
fun MinkaSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Snackbar(
        snackbarData = data,
        modifier = modifier
            .padding(MinkaSpacing.s16)
            .minkaShadow(MinkaElevation.lg, shape),
        shape = shape,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        actionColor = MaterialTheme.colorScheme.inversePrimary,
    )
}

@PreviewLightDark
@Composable
private fun MinkaSnackbarPreview() {
    MinkaTheme {
        val shape = MaterialTheme.shapes.medium
        Snackbar(
            modifier = Modifier.padding(MinkaSpacing.s16).minkaShadow(MinkaElevation.lg, shape),
            shape = shape,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            action = { Text("Deshacer", color = MaterialTheme.colorScheme.inversePrimary) },
        ) {
            Text("Publicación guardada", modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}
