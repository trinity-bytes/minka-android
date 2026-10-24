package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Tones of the Figma Badge (`296:27`). */
enum class MinkaBadgeTone { Brand, Lavender, Amber, Error, Neutral }

/** Pill status label: category, condition or points. */
@Composable
fun MinkaBadge(text: String, modifier: Modifier = Modifier, tone: MinkaBadgeTone = MinkaBadgeTone.Brand) {
    val (container, content) = tone.colors()
    Text(
        text = text,
        modifier = modifier
            .background(container, MinkaShapes.full)
            .padding(horizontal = MinkaSpacing.s8, vertical = MinkaSpacing.s4),
        color = content,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
@ReadOnlyComposable
private fun MinkaBadgeTone.colors(): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        MinkaBadgeTone.Brand -> MinkaTheme.colors.brand to MinkaTheme.colors.onBrand
        MinkaBadgeTone.Lavender -> scheme.secondaryContainer to scheme.onSecondaryContainer
        MinkaBadgeTone.Amber -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        MinkaBadgeTone.Error -> scheme.errorContainer to scheme.onErrorContainer
        MinkaBadgeTone.Neutral -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
    }
}

@PreviewLightDark
@Composable
private fun MinkaBadgePreview() {
    MinkaTheme {
        Surface {
            Row(
                modifier = Modifier.padding(MinkaSpacing.s16),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                MinkaBadgeTone.entries.forEach { MinkaBadge(text = "Etiqueta", tone = it) }
            }
        }
    }
}
