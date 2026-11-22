package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchContentTest {
    @get:Rule val compose = createComposeRule()

    private fun show(
        state: SearchUiState,
        actions: SearchActions = SearchActions(),
        onOpenListing: (String) -> Unit = {},
    ) = compose.setContent { MinkaTheme { SearchContent(state, actions, onOpenListing) } }

    @Test fun recentsAndCategoriesShowWithoutSavedSearches() {
        var picked = ""
        var cleared = false
        show(
            SearchUiState(recents = listOf("bicicleta", "taladro")),
            SearchActions(onSelectRecent = { picked = it }, onClearRecents = { cleared = true }),
        )
        compose.onNodeWithText("Recientes").assertIsDisplayed()
        compose.onNodeWithText("Categorías").assertIsDisplayed()
        compose.onNodeWithText("Tecnología").assertIsDisplayed()
        compose.onNodeWithText("Guardadas").assertDoesNotExist()
        compose.onNodeWithText("bicicleta").performClick()
        compose.onNodeWithText("Borrar").performClick()
        compose.runOnIdle {
            assertEquals("bicicleta", picked)
            assertEquals(true, cleared)
        }
    }

    @Test fun categoryDiscSearchesThatCategory() {
        var chosen: Category? = null
        show(SearchUiState(), SearchActions(onSelectCategory = { chosen = it }))
        compose.onNodeWithText("Plantas").performClick()
        compose.runOnIdle { assertEquals(Category.PLANTS, chosen) }
    }

    @Test fun suggestionsShowCategoryAndNearbyCount() {
        var picked = ""
        show(
            SearchUiState(
                text = "bici",
                stage = SearchStage.Suggest,
                suggestions = listOf(SearchSuggestion("bicicleta", Category.SPORTS, nearbyCount = 18)),
            ),
            SearchActions(onSelectRecent = { picked = it }),
        )
        compose.onNodeWithText("En Deportes · 18 cerca").assertIsDisplayed()
        compose.onNodeWithText("bicicleta").performClick()
        compose.runOnIdle { assertEquals("bicicleta", picked) }
    }

    @Test fun resultsShowTheCountAndOpenAListingWithoutTheMapToggle() {
        var opened = ""
        show(
            SearchUiState(
                text = "bici",
                stage = SearchStage.Results,
                results = listOf(listing("one", "Bicicleta de montaña"), listing("two", "Bicicleta urbana")),
                filters = SearchFilters(condition = Condition.GOOD, distanceKm = 3.0),
            ),
            onOpenListing = { opened = it },
        )
        compose.onNodeWithText("2 resultados").assertIsDisplayed()
        compose.onNodeWithText("Filtros · 2").assertIsDisplayed()
        compose.onNodeWithText("≤ 3 km").assertIsDisplayed()
        compose.onNodeWithText("Buen estado").assertIsDisplayed()
        compose.onNodeWithText("Mapa").assertDoesNotExist()
        compose.onNodeWithText("Lista").assertDoesNotExist()
        compose.onNodeWithText("Bicicleta de montaña").performClick()
        compose.runOnIdle { assertEquals("one", opened) }
    }

    @Test fun emptyResultsOfferToRemoveFiltersAndNeverSaveTheSearch() {
        var removed = false
        show(
            SearchUiState(
                text = "patines",
                stage = SearchStage.Results,
                filters = SearchFilters(distanceKm = 1.0),
            ),
            SearchActions(onRemoveFilters = { removed = true }),
        )
        compose.onNodeWithText("Sin resultados para \"patines\"").assertIsDisplayed()
        compose.onNodeWithText("Guardar búsqueda").assertDoesNotExist()
        compose.onNodeWithText("Quitar filtros").performClick()
        compose.runOnIdle { assertEquals(true, removed) }
    }

    @Test fun emptyResultsWithoutFiltersHaveNoAction() {
        show(SearchUiState(text = "patines", stage = SearchStage.Results))
        compose.onNodeWithText("Sin resultados para \"patines\"").assertIsDisplayed()
        compose.onNodeWithText("Quitar filtros").assertDoesNotExist()
    }

    @Test fun filterSheetShowsOnlyPhase3ControlsAndTheResultCount() {
        var applied = false
        show(
            SearchUiState(
                text = "bici",
                stage = SearchStage.Results,
                results = listOf(listing("one", "Bicicleta")),
                filterSheet = FilterSheetState(draft = SearchFilters(), count = 24),
            ),
            SearchActions(filterSheet = FilterSheetActions(onApply = { applied = true })),
        )
        compose.onNodeWithText("Distancia · sin límite").assertIsDisplayed()
        compose.onNodeWithText("Estado mínimo").assertIsDisplayed()
        compose.onNodeWithText("Más cerca").assertIsDisplayed()
        compose.onNodeWithText("Más reciente").assertIsDisplayed()
        compose.onNodeWithText("Reputación mínima").assertDoesNotExist()
        compose.onNodeWithText("Mejor calificado").assertDoesNotExist()
        compose.onNodeWithText("Ver 24 resultados").performClick()
        compose.runOnIdle { assertEquals(true, applied) }
    }

    private fun listing(id: String, title: String) = Listing(
        id = id,
        cover = "cover",
        morePhotos = emptyList(),
        title = title,
        category = Category.SPORTS,
        condition = Condition.GOOD,
        wants = listOf(Category.BOOKS),
        district = "Miraflores",
        status = ListingStatus.ACTIVE,
        owner = OwnerSummary(id = "owner", name = "Carlos", district = "Barranco"),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        distanceKm = 1.2,
    )
}
