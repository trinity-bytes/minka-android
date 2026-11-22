package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Expanded counterpart to [MinkaNavigationBar], with a caller-supplied primary action. */
@Composable
fun MinkaNavigationRail(
    items: List<MinkaNavigationItem>,
    header: @Composable () -> Unit,
) {
    val colors = minkaNavigationColors()
    NavigationRail(
        modifier = Modifier.width(96.dp),
        containerColor = colors.container,
        header = {
            Spacer(Modifier.height(16.dp))
            header()
            Spacer(Modifier.height(28.dp))
        },
    ) {
        items.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationRailItemDefaults.colors(
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
