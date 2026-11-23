package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Figma Foundations > Forma: large, soft radii. Buttons and chips are pills (full), cards use
// extraLarge, highlighted blocks use bento and inner controls 12–16 dp.

internal val MinkaMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Radii with no Material 3 slot. */
object MinkaShapes {
    /** Inner controls such as text fields. */
    val control = RoundedCornerShape(12.dp)

    /** Highlighted bento blocks. */
    val bento = RoundedCornerShape(38.dp)

    /** Pills: buttons and chips. */
    val full = CircleShape
}
