package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma Text field (`297:32`): outlined, 56 dp, radius medium, with the label above the field and
 * the supporting text below it. In error the supporting text carries the message.
 */
@Composable
fun MinkaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isError) scheme.error else scheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics {
                    contentDescription = label
                    if (isError) error(supportingText ?: label)
                },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
            trailingIcon = trailingIcon ?: if (isError) {
                { Icon(Icons.Rounded.Error, contentDescription = null) }
            } else {
                null
            },
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MinkaTheme.colors.raised,
                unfocusedContainerColor = MinkaTheme.colors.raised,
                errorContainerColor = MinkaTheme.colors.raised,
                disabledContainerColor = MinkaTheme.colors.raised,
                unfocusedBorderColor = scheme.outline,
                focusedBorderColor = scheme.primary,
                errorTrailingIconColor = scheme.error,
            ),
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) scheme.error else scheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MinkaTextFieldPreview() {
    MinkaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                MinkaTextField(
                    value = "Bicicleta de montaña",
                    onValueChange = {},
                    label = "Título de la publicación",
                    supportingText = "Texto de ayuda",
                )
                MinkaTextField(
                    value = "Bicicleta de montaña",
                    onValueChange = {},
                    label = "Título de la publicación",
                    supportingText = "Texto de ayuda",
                    isError = true,
                )
            }
        }
    }
}
