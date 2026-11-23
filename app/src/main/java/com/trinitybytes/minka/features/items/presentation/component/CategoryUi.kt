package com.trinitybytes.minka.features.items.presentation.component

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.RoomService
import androidx.compose.ui.graphics.vector.ImageVector
import com.trinitybytes.minka.R
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition

/** The label of a [Category], as Figma 3.01 and 5.06 write it. */
@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.HOME -> R.string.category_home
        Category.CLOTHING -> R.string.category_clothing
        Category.BOOKS -> R.string.category_books
        Category.TECHNOLOGY -> R.string.category_technology
        Category.PLANTS -> R.string.category_plants
        Category.SPORTS -> R.string.category_sports
        Category.TOOLS -> R.string.category_tools
        Category.SERVICES -> R.string.category_services
        Category.ANY -> R.string.category_any
    }

/** The icon of a [Category]: the Figma 3.01 icons, plus the ones 5.06 adds for the "Busca:" chips. */
val Category.icon: ImageVector
    get() = when (this) {
        Category.HOME -> Icons.Rounded.Chair
        Category.CLOTHING -> Icons.Rounded.Checkroom
        Category.BOOKS -> Icons.Rounded.MenuBook
        Category.TECHNOLOGY -> Icons.Rounded.Devices
        Category.PLANTS -> Icons.Rounded.LocalFlorist
        Category.SPORTS -> Icons.Rounded.PedalBike
        Category.TOOLS -> Icons.Rounded.Handyman
        Category.SERVICES -> Icons.Rounded.RoomService
        Category.ANY -> Icons.Rounded.AllInclusive
    }

/**
 * What an item can be filed under: the six categories of Figma 3.01, in its order. Tools, services
 * and "Lo que sea" only exist as something a person wants in exchange (5.06).
 */
val ItemCategories: List<Category> = listOf(
    Category.HOME,
    Category.CLOTHING,
    Category.BOOKS,
    Category.TECHNOLOGY,
    Category.PLANTS,
    Category.SPORTS,
)

/** The "Busca:" chips of Figma 5.06, in its order. */
val WantedCategories: List<Category> = listOf(
    Category.BOOKS,
    Category.CLOTHING,
    Category.HOME,
    Category.TOOLS,
    Category.PLANTS,
    Category.SERVICES,
    Category.ANY,
)

/** The label of a [Condition] ("Estado", Figma 5.01). */
@get:StringRes
val Condition.labelRes: Int
    get() = when (this) {
        Condition.NEW -> R.string.condition_new
        Condition.LIKE_NEW -> R.string.condition_like_new
        Condition.GOOD -> R.string.condition_good
        Condition.USED -> R.string.condition_used
    }
