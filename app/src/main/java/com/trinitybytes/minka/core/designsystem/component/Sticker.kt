package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.em
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Tones of the Figma Sticker (`344:83`). */
enum class MinkaStickerTone {
    /** Card paper with body text; follows the theme. */
    Paper,

    /** Ink paper with brand text; keeps the light look in every theme, as on the landing. */
    Ink,
}

/**
 * Flat tilted pill label: Label/Sticker, uppercase, no border or shadow. Figma rotates it between
 * −6° and +6°.
 */
@Composable
fun MinkaSticker(
    text: String,
    modifier: Modifier = Modifier,
    tone: MinkaStickerTone = MinkaStickerTone.Paper,
    rotation: Float = 0f,
) {
    val (container, content) = tone.colors()
    Text(
        text = text.toUpperCase(Locale.current),
        modifier = modifier
            .rotate(rotation)
            .background(container, MinkaShapes.full)
            .padding(horizontal = MinkaSpacing.s16, vertical = MinkaSpacing.s8),
        color = content,
        // Figma tracks the sticker at 6 % of the font size (0.78 px at 13 px).
        style = MinkaTheme.typography.sticker.copy(letterSpacing = StickerTracking),
        maxLines = 1,
    )
}

private val StickerTracking = 0.06.em

// The Ink tone stays light-mode ink in every theme (Figma surface/inverse in Light).
private val InkPaper = Color(0xFF10291C)

@Composable
@ReadOnlyComposable
private fun MinkaStickerTone.colors(): Pair<Color, Color> = when (this) {
    MinkaStickerTone.Paper -> MinkaTheme.colors.raised to MaterialTheme.colorScheme.onSurface
    MinkaStickerTone.Ink -> InkPaper to MinkaTheme.colors.brand
}

@PreviewLightDark
@Composable
private fun MinkaStickerPreview() {
    MinkaTheme {
        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s24),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s24),
            ) {
                MinkaSticker(text = "Sin dinero de por medio", rotation = -4f)
                MinkaSticker(text = "Directo con vecinos", tone = MinkaStickerTone.Ink, rotation = 5f)
            }
        }
    }
}
