package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
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

/** Heights of the Figma Button. */
enum class MinkaButtonSize {
    /** 40 dp with Label/Large: the default. */
    Medium,

    /** 56 dp with Title/Medium: the main actions at the bottom of a screen. */
    Large,
}

/**
 * Pill button, 40 dp tall (56 dp when [size] is Large), with an optional 18 dp leading icon. While
 * [loading] it keeps its colors, ignores clicks and shows the loading indicator instead of the
 * label (Figma 1.07: 36 dp indicator on a Large button).
 */
@Composable
fun MinkaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MinkaButtonStyle = MinkaButtonStyle.Filled,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    size: MinkaButtonSize = MinkaButtonSize.Medium,
    loading: Boolean = false,
) {
    val isOutlined = style == MinkaButtonStyle.Outlined
    val isLarge = size == MinkaButtonSize.Large
    Button(
        onClick = onClick,
        modifier = if (isLarge) modifier.heightIn(min = LargeHeight) else modifier,
        enabled = enabled && !loading,
        shape = MinkaShapes.full,
        colors = buttonColors(style, loading),
        border = if (isOutlined) outlinedBorder(enabled) else null,
        contentPadding = if (style == MinkaButtonStyle.Text) TextContentPadding else ButtonDefaults.ContentPadding,
    ) {
        if (loading) {
            MinkaLoadingIndicator(
                modifier = Modifier
                    .size(if (isLarge) LargeLoadingSize else ButtonDefaults.IconSize)
                    .semantics { contentDescription = text },
                color = LocalContentColor.current,
            )
            return@Button
        }
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(text, style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
    }
}

private val LargeHeight = 56.dp
private val LargeLoadingSize = 36.dp

private val TextContentPadding = PaddingValues(horizontal = MinkaSpacing.s12, vertical = MinkaSpacing.s8)

@Composable
@ReadOnlyComposable
private fun buttonColors(style: MinkaButtonStyle, loading: Boolean): ButtonColors {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (style) {
        MinkaButtonStyle.Filled -> MinkaTheme.colors.brand to MinkaTheme.colors.onBrand
        MinkaButtonStyle.Ink -> scheme.inverseSurface to scheme.inverseOnSurface
        MinkaButtonStyle.Tonal -> scheme.secondaryContainer to scheme.onSecondaryContainer
        MinkaButtonStyle.Outlined, MinkaButtonStyle.Text -> Color.Transparent to scheme.primary
        MinkaButtonStyle.Destructive -> scheme.error to scheme.onError
    }
    val hasContainer = container != Color.Transparent
    if (loading) return ButtonColors(container, content, container, content)
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

@Preview
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
                MinkaButton(text = "Crear cuenta", onClick = {}, size = MinkaButtonSize.Large)
                MinkaButton(text = "Ingresar", onClick = {}, size = MinkaButtonSize.Large, loading = true)
            }
        }
    }
}
