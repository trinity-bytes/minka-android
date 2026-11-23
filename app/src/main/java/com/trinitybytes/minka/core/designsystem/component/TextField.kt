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
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma Text field (`297:32`): outlined, 56 dp, radius medium, with the label above the field and
 * the supporting text below it. In error the supporting text carries the message. The visible label
 * is hidden from accessibility because the field itself carries it, so TalkBack reads it once.
 *
 * With [secure] the value is masked and a 48 dp trailing toggle shows or hides it (Figma property
 * "Visibility toggle"); the toggle replaces the error icon, so it stays reachable in error.
 * [visualTransformation] and [trailingIcon] are ignored in that mode.
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
    secure: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    var revealed by rememberSaveable { mutableStateOf(false) }
    val trailing: @Composable (() -> Unit)? = when {
        secure -> {
            { PasswordVisibilityToggle(revealed = revealed, onToggle = { revealed = !revealed }, enabled = enabled) }
        }
        trailingIcon != null -> trailingIcon
        isError -> {
            { Icon(Icons.Rounded.Error, contentDescription = null) }
        }
        else -> null
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
        Text(
            text = label,
            modifier = Modifier.semantics { hideFromAccessibility() },
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
            trailingIcon = trailing,
            isError = isError,
            visualTransformation = when {
                !secure -> visualTransformation
                revealed -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
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

/** Shows or hides a [MinkaTextField] in secure mode; the icon button keeps the 48 dp target. */
@Composable
private fun PasswordVisibilityToggle(revealed: Boolean, onToggle: () -> Unit, enabled: Boolean) {
    IconButton(onClick = onToggle, enabled = enabled) {
        Icon(
            imageVector = if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = stringResource(if (revealed) R.string.password_hide else R.string.password_show),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
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
                MinkaTextField(
                    value = "Minka2026",
                    onValueChange = {},
                    label = "Contraseña",
                    supportingText = "Credenciales incorrectas",
                    isError = true,
                    secure = true,
                )
            }
        }
    }
}
