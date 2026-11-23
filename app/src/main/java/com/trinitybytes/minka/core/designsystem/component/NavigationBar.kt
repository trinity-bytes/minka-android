package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** A destination shared by the compact bar and expanded rail. */
data class MinkaNavigationItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

internal data class MinkaNavigationColors(
    val container: Color,
    val indicator: Color,
    val selectedIcon: Color,
    val selectedText: Color,
    val unselected: Color,
)

@Composable
internal fun minkaNavigationColors(): MinkaNavigationColors = MinkaNavigationColors(
    container = MaterialTheme.colorScheme.surfaceContainerLowest,
    indicator = MaterialTheme.colorScheme.inverseSurface,
    selectedIcon = MaterialTheme.colorScheme.inverseOnSurface,
    selectedText = MaterialTheme.colorScheme.onSurface,
    unselected = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Compact navigation with the Figma selection and surface colors. */
@Composable
fun MinkaNavigationBar(items: List<MinkaNavigationItem>) {
    val colors = minkaNavigationColors()
    NavigationBar(containerColor = colors.container) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = colors.indicator,
                    selectedIconColor = colors.selectedIcon,
                    selectedTextColor = colors.selectedText,
                    unselectedIconColor = colors.unselected,
                    unselectedTextColor = colors.unselected,
                ),
            )
        }
    }
}
