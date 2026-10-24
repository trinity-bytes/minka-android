package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Emphasis levels of the Figma Button (`296:76`). */
enum class MinkaButtonStyle {
    /** Brand green: the main action of a screen. */
    Filled,

    /** High-emphasis dark. */
    Ink,

    /** Lavender secondary. */
    Tonal,

    /** Low emphasis with an outline. */
    Outlined,

    /** Lowest emphasis. */
    Text,

    /** Irreversible actions such as deleting the account or blocking someone. */
    Destructive,
}

/** Pill button, 40 dp tall, with an optional 18 dp leading icon. */
@Composable
fun MinkaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MinkaButtonStyle = MinkaButtonStyle.Filled,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val isOutlined = style == MinkaButtonStyle.Outlined
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MinkaShapes.full,
        colors = buttonColors(style),
        border = if (isOutlined) outlinedBorder(enabled) else null,
        contentPadding = if (style == MinkaButtonStyle.Text) TextContentPadding else ButtonDefaults.ContentPadding,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

private val TextContentPadding = PaddingValues(horizontal = MinkaSpacing.s12, vertical = MinkaSpacing.s8)

@Composable
@ReadOnlyComposable
private fun buttonColors(style: MinkaButtonStyle): ButtonColors {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (style) {
        MinkaButtonStyle.Filled -> MinkaTheme.colors.brand to MinkaTheme.colors.onBrand
        MinkaButtonStyle.Ink -> scheme.inverseSurface to scheme.inverseOnSurface
        MinkaButtonStyle.Tonal -> scheme.secondaryContainer to scheme.onSecondaryContainer
        MinkaButtonStyle.Outlined, MinkaButtonStyle.Text -> Color.Transparent to scheme.primary
        MinkaButtonStyle.Destructive -> scheme.error to scheme.onError
    }
    val hasContainer = container != Color.Transparent
    return ButtonColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = if (hasContainer) scheme.onSurface.copy(alpha = DisabledContainerAlpha) else Color.Transparent,
        disabledContentColor = scheme.onSurface.copy(alpha = DisabledContentAlpha),
    )
}

@Composable
@ReadOnlyComposable
private fun outlinedBorder(enabled: Boolean) = BorderStroke(
    width = 1.dp,
    color = if (enabled) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
    },
)

// Figma state/disabled-container and state/disabled-content (on-surface at 12% and 38%).
internal const val DisabledContainerAlpha = 0.12f
internal const val DisabledContentAlpha = 0.38f

@PreviewLightDark
@Composable
private fun MinkaButtonPreview() {
    MinkaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                MinkaButtonStyle.entries.forEach { style ->
                    MinkaButton(text = "Proponer trueque", onClick = {}, style = style, leadingIcon = Icons.Rounded.Add)
                }
                MinkaButton(text = "Proponer trueque", onClick = {}, enabled = false)
            }
        }
    }
}
