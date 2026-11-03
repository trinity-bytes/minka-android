package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma Chip (`296:91`), a 32 dp filter pill. Selected: dark ink (inverse surface) with a check,
 * as in the web app; otherwise card paper with an outline.
 */
@Composable
fun MinkaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(32.dp)
            .semantics {
                this.selected = selected
                role = Role.Checkbox
            },
        enabled = enabled,
        shape = MinkaShapes.full,
        color = if (selected) scheme.inverseSurface else MinkaTheme.colors.raised,
        contentColor = if (selected) scheme.inverseOnSurface else scheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = if (selected) MinkaSpacing.s8 else MinkaSpacing.s16, end = MinkaSpacing.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
        ) {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@PreviewLightDark
@Composable
private fun MinkaFilterChipPreview() {
    MinkaTheme {
        Surface {
            FlowRow(
                modifier = Modifier.padding(MinkaSpacing.s16),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                MinkaFilterChip(label = "Hogar", selected = false, onClick = {})
                MinkaFilterChip(label = "Libros", selected = true, onClick = {})
            }
        }
    }
}
