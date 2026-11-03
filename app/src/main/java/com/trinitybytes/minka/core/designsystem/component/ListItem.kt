package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Figma Avatar (`296:16`): initials on lavender, 40 dp. */
@Composable
fun MinkaAvatar(initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** Up to two initials for [MinkaAvatar]: the first letters of the first and last words of [name]. */
fun initialsOf(name: String): String {
    val words = name.split(' ').filter { it.firstOrNull()?.isLetter() == true }
    if (words.isEmpty()) return ""
    val letters = if (words.size == 1) listOf(words.first()) else listOf(words.first(), words.last())
    return letters.joinToString("") { it.first().uppercase() }
}

/** Rounded group that holds [MinkaListItem] rows, as in the Bottom sheet content slot. */
@Composable
fun MinkaListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

/**
 * Figma List item (`299:82`): 72 dp row on the raised surface with a leading [MinkaAvatar]
 * ([avatarInitials]) or a 40 dp icon wrap ([leadingIcon]), one or two lines and an optional
 * trailing icon.
 */
@Composable
fun MinkaListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    avatarInitials: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(MinkaTheme.colors.raised)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = MinkaSpacing.s16),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            avatarInitials != null -> MinkaAvatar(avatarInitials)
            leadingIcon != null -> Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(scheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(leadingIcon, contentDescription = null, tint = scheme.onSurface)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
        }
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

@PreviewLightDark
@Composable
private fun MinkaListItemPreview() {
    MinkaTheme {
        Surface(color = MinkaTheme.colors.overlay) {
            MinkaListGroup(modifier = Modifier.padding(MinkaSpacing.s24)) {
                MinkaListItem(
                    headline = "Ana Quispe",
                    supporting = "ana.quispe@gmail.com",
                    avatarInitials = initialsOf("Ana Quispe"),
                    onClick = {},
                )
                MinkaListItem(
                    headline = "Agregar otra cuenta",
                    leadingIcon = Icons.Rounded.PersonAdd,
                    trailingIcon = Icons.Rounded.ChevronRight,
                    onClick = {},
                )
            }
        }
    }
}
