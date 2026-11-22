package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.component.MinkaExpressiveShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.presentation.component.icon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A closed shape whose radius swings [lobes] times between `1 - depth` and 1 of the half-width: the Clover
 * (4 lobes) and Sunny (8 lobes) of the Figma Shape component, which the design system does not carry.
 */
private class LobedShape(private val lobes: Int, private val depth: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = min(centerX, centerY)
        val path = Path()
        for (step in 0..OUTLINE_STEPS) {
            val angle = 2.0 * PI * step / OUTLINE_STEPS
            val swing = 1f - depth / 2 + depth / 2 * cos(lobes * angle).toFloat()
            val x = centerX + radius * swing * cos(angle).toFloat()
            val y = centerY + radius * swing * sin(angle).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    private companion object {
        const val OUTLINE_STEPS = 240
    }
}

private val Clover: Shape = LobedShape(lobes = 4, depth = 0.3f)
private val Sunny: Shape = LobedShape(lobes = 8, depth = 0.1f)
private val Arch: Shape = RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
private val Squircle: Shape = RoundedCornerShape(40.dp)

/** Static accent colors per category (audit finding E3); `secondary/container` stays for "Busca:" chips. */
private data class DiscStyle(val shape: Shape, val container: Color, val content: Color)

@Composable
private fun Category.discStyle(): DiscStyle {
    val colors = MinkaTheme.colors
    return when (this) {
        Category.HOME -> DiscStyle(MinkaExpressiveShapes.Cookie12, colors.amber, colors.onAmber)
        Category.CLOTHING -> DiscStyle(Clover, colors.coral, colors.onCoral)
        Category.BOOKS -> DiscStyle(Arch, colors.sky, colors.onSky)
        Category.TECHNOLOGY -> DiscStyle(
            Sunny,
            colors.lavenderContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Category.PLANTS -> DiscStyle(Squircle, colors.mint, colors.onBrand)
        else -> DiscStyle(CircleShape, colors.skyContainer, colors.onSkyContainer)
    }
}

/** The 76 dp category art of Figma 3.01: an expressive shape on a static accent color with the category icon. */
@Composable
internal fun CategoryDisc(category: Category, modifier: Modifier = Modifier) {
    val style = category.discStyle()
    Box(
        modifier = modifier.size(76.dp).background(style.container, style.shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = style.content)
    }
}
