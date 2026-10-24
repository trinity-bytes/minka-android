package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Figma Foundations > Elevación. The language is flat: most surfaces separate by color
 * (surface vs. container), and the soft ink-tinted shadow only marks a layer.
 */
object MinkaElevation {
    /** Resting cards, when they need any shadow at all. */
    val sm = Shadow(radius = 2.dp, color = Color(0x0D10291C), offset = DpOffset(0.dp, 1.dp))

    /** Raised chips and the search bar. */
    val md = Shadow(radius = 8.dp, color = Color(0x0F10291C), offset = DpOffset(0.dp, 2.dp))

    /** Menus and the FAB. */
    val lg = Shadow(radius = 24.dp, color = Color(0x1F10291C), spread = (-8).dp, offset = DpOffset(0.dp, 8.dp))

    /** Bottom sheets. */
    val xl = Shadow(radius = 40.dp, color = Color(0x2910291C), spread = (-12).dp, offset = DpOffset(0.dp, 16.dp))

    /** Pressed or dragged cards. */
    val lift = Shadow(radius = 28.dp, color = Color(0x2E10291C), spread = (-10).dp, offset = DpOffset(0.dp, 10.dp))

    /** Dialogs. */
    val modal = Shadow(radius = 64.dp, color = Color(0x470A1D13), spread = (-16).dp, offset = DpOffset(0.dp, 24.dp))
}

/** Draws one of the [MinkaElevation] shadows behind content clipped to [shape]. */
fun Modifier.minkaShadow(elevation: Shadow, shape: Shape): Modifier = dropShadow(shape, elevation)
