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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.trinitybytes.minka.core.designsystem.theme.MinkaElevation
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.core.designsystem.theme.minkaShadow

/**
 * Figma Dialog (`299:102`): overlay surface, radius extraLarge and the modal shadow, with an
 * optional icon (secondary unless [iconTint] says otherwise), a Fraunces headline and the actions
 * at the end. [confirmStyle] is a text button by default, as M3 dialogs ask for non-destructive
 * actions; pass [MinkaButtonStyle.Destructive] for irreversible ones. The [dismissText] button calls [onDismissClick], which defaults to [onDismiss] (the
 * scrim and back gesture); pass it when that button leads somewhere else.
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
    confirmStyle: MinkaButtonStyle = MinkaButtonStyle.Text,
    iconTint: Color = MaterialTheme.colorScheme.secondary,
    onDismissClick: () -> Unit = onDismiss,
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
                    Icon(icon, contentDescription = null, tint = iconTint)
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8, alignment = Alignment.End),
                ) {
                    if (dismissText != null) {
                        MinkaButton(text = dismissText, onClick = onDismissClick, style = MinkaButtonStyle.Text)
                    }
                    MinkaButton(
                        text = confirmText,
                        onClick = onConfirm,
                        style = confirmStyle,
                    )
                }
            }
        }
    }
}

@Preview
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
