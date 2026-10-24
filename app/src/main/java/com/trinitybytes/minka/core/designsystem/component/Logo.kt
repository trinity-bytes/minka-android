package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Variants of the Figma Logo (`331:2929`). */
enum class MinkaLogoType {
    /** "mink'a" in Fraunces Black with the two-leaf apostrophe. */
    Logotype,

    /** The leaves alone. */
    Logomark,
}

/**
 * Minka logo, scaled to the height the [modifier] gives it (24 dp minimum per Figma). The leaf is
 * brand green except on lavender or on the brand green itself, where callers pass [leafColor].
 */
@Composable
fun MinkaLogo(
    modifier: Modifier = Modifier,
    type: MinkaLogoType = MinkaLogoType.Logotype,
    glyphColor: Color = MaterialTheme.colorScheme.onSurface,
    leafColor: Color = MinkaTheme.colors.brand,
) {
    val description = stringResource(R.string.app_name)
    val semantics = Modifier.semantics { contentDescription = description }
    when (type) {
        MinkaLogoType.Logotype -> Box(modifier.aspectRatio(LogotypeRatio).then(semantics)) {
            Image(
                painter = painterResource(R.drawable.logotype_glyphs),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                colorFilter = ColorFilter.tint(glyphColor),
            )
            Image(
                painter = painterResource(R.drawable.logotype_leaf),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                colorFilter = ColorFilter.tint(leafColor),
            )
        }
        MinkaLogoType.Logomark -> Image(
            painter = painterResource(R.drawable.logomark),
            contentDescription = null,
            modifier = modifier.aspectRatio(LogomarkRatio).then(semantics),
            colorFilter = ColorFilter.tint(leafColor),
        )
    }
}

private const val LogotypeRatio = 294f / 85f
private const val LogomarkRatio = 56f / 65f

@PreviewLightDark
@Composable
private fun MinkaLogoPreview() {
    MinkaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                MinkaLogo(Modifier.height(48.dp))
                MinkaLogo(Modifier.height(48.dp), type = MinkaLogoType.Logomark)
            }
        }
    }
}
