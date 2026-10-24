package com.trinitybytes.minka.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Password strength as the Figma Strength meter (`381:212`) shows it. */
enum class StrengthLevel(val filledSegments: Int, @StringRes internal val labelRes: Int, internal val icon: ImageVector) {
    Weak(1, R.string.strength_weak, Icons.Rounded.Error),
    Medium(2, R.string.strength_medium, Icons.Rounded.Info),
    Strong(SEGMENTS, R.string.strength_strong, Icons.Rounded.CheckCircle),
}

private const val SEGMENTS = 4

/** Four segments plus an icon and a label, under the password field. */
@Composable
fun MinkaStrengthMeter(level: StrengthLevel, modifier: Modifier = Modifier) {
    val color = level.color()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(SEGMENTS) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (index < level.filledSegments) color else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(3.dp),
                    ),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
            Icon(level.icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(stringResource(level.labelRes), style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

@Composable
@ReadOnlyComposable
private fun StrengthLevel.color(): Color = when (this) {
    StrengthLevel.Weak -> MaterialTheme.colorScheme.error
    StrengthLevel.Medium -> MaterialTheme.colorScheme.tertiary
    StrengthLevel.Strong -> MaterialTheme.colorScheme.primary
}

@PreviewLightDark
@Composable
private fun MinkaStrengthMeterPreview() {
    MinkaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                StrengthLevel.entries.forEach { MinkaStrengthMeter(level = it, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}
