package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.component.MinkaButton
import com.trinitybytes.minka.core.designsystem.component.MinkaButtonStyle
import com.trinitybytes.minka.core.designsystem.component.MinkaListGroup
import com.trinitybytes.minka.core.designsystem.component.MinkaListItem
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.presentation.component.ItemCategories
import com.trinitybytes.minka.features.items.presentation.component.labelRes

/** Figma 3.01: "Recientes" with "Borrar", then the six categories. "Guardadas" waits for saved searches. */
@Composable
internal fun BrowseContent(
    recents: List<String>,
    onSelectRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onSelectCategory: (Category) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = MinkaSpacing.s16, end = MinkaSpacing.s16, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
    ) {
        if (recents.isNotEmpty()) recentItems(recents, onSelectRecent, onClearRecents)
        item(key = "categories-title", contentType = "title") {
            SectionTitle(stringResource(R.string.search_categories))
        }
        item(key = "categories", contentType = "categories") { CategoryGrid(onSelectCategory) }
    }
}

private fun LazyListScope.recentItems(
    recents: List<String>,
    onSelectRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
) {
    item(key = "recents-title", contentType = "title") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.search_recents), Modifier.weight(1f))
            MinkaButton(stringResource(R.string.search_recents_clear), onClearRecents, style = MinkaButtonStyle.Text)
        }
    }
    item(key = "recents", contentType = "recents") {
        MinkaListGroup {
            recents.forEach { text ->
                MinkaListItem(headline = text, leadingIcon = Icons.Rounded.History, onClick = { onSelectRecent(text) })
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun CategoryGrid(onSelectCategory: (Category) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CATEGORY_GAP, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
    ) {
        ItemCategories.forEach { category ->
            Column(
                modifier = Modifier
                    .width(CATEGORY_WIDTH)
                    .clickable(role = Role.Button) { onSelectCategory(category) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CategoryDisc(category)
                Text(
                    stringResource(category.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private val CATEGORY_WIDTH = 78.dp

/** Figma 3.01 places the 78 dp category columns on a 100 dp pitch. */
private val CATEGORY_GAP = 22.dp

/** Figma 3.02: one row per title that matches, with its category and how many are near. */
@Composable
internal fun SuggestionsContent(
    suggestions: List<SearchSuggestion>,
    onSelect: (String) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = MinkaSpacing.s16, end = MinkaSpacing.s16, bottom = bottomPadding),
    ) {
        if (suggestions.isNotEmpty()) {
            item(key = "suggestions", contentType = "suggestions") {
                MinkaListGroup {
                    suggestions.forEach { suggestion ->
                        MinkaListItem(
                            headline = suggestion.title,
                            supporting = suggestion.supporting(),
                            leadingIcon = Icons.Rounded.Search,
                            onClick = { onSelect(suggestion.title) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestion.supporting(): String? {
    val nearby = nearbyCount.takeIf { it > 0 }?.let { stringResource(R.string.search_suggestion_nearby, it) }
    val inCategory = category?.let { stringResource(R.string.search_suggestion_category, stringResource(it.labelRes)) }
    return listOfNotNull(inCategory, nearby).joinToString(" · ").ifEmpty { null }
}
