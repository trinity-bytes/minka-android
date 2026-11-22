package com.trinitybytes.minka.features.items.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchAndEmptyActionOpenSearch() {
        var calls = 0
        compose.setContent {
            MinkaTheme {
                HomeContent(HomeUiState(loading = false, feed = Feed(emptyList(), emptyList())), { calls++ }, {}, {})
            }
        }
        compose.onNodeWithText("Buscar objetos o servicios").performClick()
        compose.onNodeWithText("Ampliar distancia").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, calls) }
    }

    @Test fun cachedFeedIsReadableAndSearchIsDisabled() {
        val listing = listing()
        compose.setContent {
            val state = HomeUiState(loading = false, feed = Feed(listOf(listing), emptyList()), offline = true)
            MinkaTheme { HomeContent(state, {}, {}, {}) }
        }
        compose.onNodeWithText("Sin conexión. Ves lo último guardado.").assertIsDisplayed()
        compose.onNodeWithText("Buscar objetos o servicios").assertIsNotEnabled()
        compose.onNodeWithText("Bicicleta").performScrollTo().assertIsDisplayed()
    }

    @Test fun tappingCardOpensItsListing() {
        var opened = ""
        compose.setContent {
            val state = HomeUiState(loading = false, feed = Feed(listOf(listing()), emptyList()))
            MinkaTheme { HomeContent(state, {}, { opened = it }, {}) }
        }
        compose.onNodeWithText("Bicicleta").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("one", opened) }
    }


    @Test fun interestShelfIsNotDisplayedAndSeeAllOpensSearch() {
        var opened = false
        compose.setContent {
            MinkaTheme {
                val feed = Feed(listOf(listing()), listOf(listing().copy(title = "Interest only")))
                HomeContent(HomeUiState(loading = false, feed = feed), { opened = true }, {}, {})
            }
        }
        compose.onNodeWithText("Interest only").assertDoesNotExist()
        compose.onNodeWithText("Para ti").assertDoesNotExist()
        compose.onNodeWithText("Ver todo").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, opened) }
    }

    @Test fun offlineSnackbarRetries() {
        var retried = false
        compose.setContent {
            MinkaTheme {
                val state = HomeUiState(loading = false, feed = Feed(listOf(listing()), emptyList()), offline = true)
                HomeContent(state, {}, {}, { retried = true })
            }
        }
        compose.onNodeWithText("Reintentar").performClick()
        compose.runOnIdle { assertEquals(true, retried) }
    }

    @Test fun todoChipIsSelectedFirstAndClearsTheFilter() {
        var all = 0
        var picked: Category? = null
        compose.setContent {
            MinkaTheme {
                val state = HomeUiState(
                    loading = false,
                    feed = Feed(listOf(listing()), emptyList()),
                    selectedCategory = Category.BOOKS,
                )
                HomeContent(state, {}, {}, {}, { picked = it }, {}, { all++ })
            }
        }
        compose.onNodeWithText("Todo").assertIsNotSelected().performClick()
        compose.onNodeWithText("Libros").assertIsSelected()
        compose.onNodeWithText("Hogar").performClick()
        compose.runOnIdle {
            assertEquals(1, all)
            assertEquals(Category.HOME, picked)
        }
    }

    @Test fun defaultStateSelectsTodoAndShowsTheCardMetaWithoutCondition() {
        compose.setContent {
            MinkaTheme {
                val state = HomeUiState(
                    loading = false,
                    feed = Feed(listOf(listing()), emptyList()),
                    district = "Miraflores",
                    initials = "AQ",
                )
                HomeContent(state, {}, {}, {})
            }
        }
        compose.onNodeWithText("Todo").assertIsSelected()
        compose.onNodeWithText("AQ").assertIsDisplayed()
        compose.onNodeWithText("Hay 1 trueques cerca de Miraflores.").assertIsDisplayed()
        compose.onNodeWithText("Miraflores · 1,2 km").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Busca: libros").performScrollTo().assertIsDisplayed()
    }

    private fun listing() = Listing(
        id = "one", cover = "", morePhotos = emptyList(), title = "Bicicleta", category = Category.SPORTS,
        condition = Condition.GOOD, wants = listOf(Category.BOOKS), district = "Miraflores",
        status = ListingStatus.ACTIVE, owner = OwnerSummary("owner", "Ana", "Miraflores"),
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH, distanceKm = 1.2,
    )
}
