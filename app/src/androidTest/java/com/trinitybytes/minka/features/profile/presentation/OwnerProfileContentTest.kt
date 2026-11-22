package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerProfile
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerProfileContentTest {
    @get:Rule val compose = createComposeRule()

    private val owner = OwnerSummary("carlos", "Carlos Ramos", "Barranco")
    private val guitar = Listing(
        id = "g",
        cover = "",
        morePhotos = emptyList(),
        title = "Guitarra acústica",
        category = Category.HOME,
        condition = Condition.GOOD,
        wants = listOf(Category.SPORTS),
        district = "Barranco",
        status = ListingStatus.ACTIVE,
        owner = owner,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @Test fun showsNameDistrictAndListingsWithoutLaterPhaseElements() {
        var opened = ""
        compose.setContent {
            MinkaTheme {
                OwnerProfileContent(
                    state = OwnerProfileUiState(loading = false, profile = OwnerProfile(owner, listOf(guitar))),
                    onBack = {},
                    onOpenListing = { opened = it },
                    onRetry = {},
                )
            }
        }
        compose.onNodeWithText("Carlos Ramos").assertIsDisplayed()
        compose.onAllNodesWithText("Barranco").onFirst().assertIsDisplayed()
        compose.onNodeWithText("Raíz Verificada").assertDoesNotExist()
        compose.onNodeWithText("Reseñas").assertDoesNotExist()
        compose.onNodeWithText("Guitarra acústica").performClick()
        compose.runOnIdle { assertEquals("g", opened) }
    }

    @Test fun memberSinceLineIsHiddenWhenUnknown() {
        compose.setContent {
            MinkaTheme {
                OwnerProfileContent(
                    state = OwnerProfileUiState(loading = false, profile = OwnerProfile(owner, emptyList())),
                    onBack = {},
                    onOpenListing = {},
                    onRetry = {},
                )
            }
        }
        compose.onNodeWithText("Barranco").assertIsDisplayed()
        compose.onNodeWithText("Carlos Ramos no tiene publicaciones activas.").assertIsDisplayed()
    }

    @Test fun notFoundOffersToGoBack() {
        var back = 0
        compose.setContent {
            MinkaTheme {
                OwnerProfileContent(
                    state = OwnerProfileUiState(loading = false, notFound = true),
                    onBack = { back++ },
                    onOpenListing = {},
                    onRetry = {},
                )
            }
        }
        compose.onNodeWithText("Este perfil no está disponible").assertIsDisplayed()
        compose.onNodeWithText("Volver").performClick()
        compose.runOnIdle { assertEquals(1, back) }
    }
}
