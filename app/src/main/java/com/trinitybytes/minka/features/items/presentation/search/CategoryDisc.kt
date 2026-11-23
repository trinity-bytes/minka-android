package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.component.MinkaExpressiveShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.presentation.component.icon

/** Static accent colors per category (audit finding E3); `secondary/container` stays for "Busca:" chips. */
private data class DiscStyle(val shape: Shape, val container: Color, val content: Color)

@Composable
private fun Category.discStyle(): DiscStyle {
    val colors = MinkaTheme.colors
    return when (this) {
        Category.HOME -> DiscStyle(MinkaExpressiveShapes.CategoryCookie, colors.amber, colors.onAmber)
        Category.CLOTHING -> DiscStyle(MinkaExpressiveShapes.CategoryClover, colors.coral, colors.onCoral)
        Category.BOOKS -> DiscStyle(MinkaExpressiveShapes.CategoryArch, colors.sky, colors.onSky)
        Category.TECHNOLOGY -> DiscStyle(
            MinkaExpressiveShapes.CategorySunny,
            colors.lavenderContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Category.PLANTS -> DiscStyle(MinkaExpressiveShapes.CategorySquircle, colors.mint, colors.onBrand)
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
