package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import java.time.Instant
import java.time.ZoneId

private val AvatarSize = 88.dp

/**
 * The colored block that opens 11.01 and 11.02: a large avatar with the initials, the name and
 * "Miraflores · desde 2025". The trades, Eco-Puntos and reviews cards, and the "Raíz Verificada"
 * badge, wait for later phases and are not drawn.
 */
@Composable
internal fun ProfileStage(
    name: String,
    initials: String,
    district: String?,
    memberSince: Instant?,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, MinkaShapes.bento)
            .padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s20),
        horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s4)) {
            Text(name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            val line = profileLine(district, memberSince)
            if (line.isNotEmpty()) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Miraflores · desde 2025"; a missing district or date is left out, and so is the separator. */
@Composable
internal fun profileLine(district: String?, memberSince: Instant?): String {
    val since = memberSince?.let {
        stringResource(R.string.profile_since, it.atZone(ZoneId.systemDefault()).year)
    }
    return listOfNotNull(district, since).joinToString(" · ")
}
