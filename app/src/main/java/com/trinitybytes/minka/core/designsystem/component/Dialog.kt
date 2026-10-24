package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.trinitybytes.minka.core.designsystem.theme.MinkaElevation
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.core.designsystem.theme.minkaShadow

/**
 * Figma Dialog (`299:102`): overlay surface, radius extraLarge and the modal shadow, with an
 * optional secondary icon, a Fraunces headline and the actions at the end. [destructive] turns
 * the confirm button into the Destructive style for irreversible actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinkaDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = modifier) {
        val shape = MaterialTheme.shapes.extraLarge
        Surface(
            modifier = Modifier.minkaShadow(MinkaElevation.modal, shape),
            shape = shape,
            color = MinkaTheme.colors.overlay,
        ) {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s24),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8, alignment = Alignment.End),
                ) {
                    if (dismissText != null) {
                        MinkaButton(text = dismissText, onClick = onDismiss, style = MinkaButtonStyle.Text)
                    }
                    MinkaButton(
                        text = confirmText,
                        onClick = onConfirm,
                        style = if (destructive) MinkaButtonStyle.Destructive else MinkaButtonStyle.Filled,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MinkaDialogPreview() {
    MinkaTheme {
        MinkaDialog(
            title = "¿Confirmar el trueque?",
            text = "Se avisará a María y el objeto quedará reservado hasta que ambos confirmen la entrega.",
            confirmText = "Confirmar",
            onConfirm = {},
            onDismiss = {},
            dismissText = "Cancelar",
            icon = Icons.Rounded.SwapHoriz,
        )
    }
}
