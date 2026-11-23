package com.trinitybytes.minka.features.items.presentation.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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
class ListingDetailScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val now = Instant.parse("2026-11-22T15:00:00Z")

    private fun listing(status: ListingStatus = ListingStatus.ACTIVE) = Listing(
        id = "l1",
        cover = "none",
        morePhotos = emptyList(),
        title = "Bicicleta de montaña aro 26",
        category = Category.SPORTS,
        condition = Condition.GOOD,
        wants = listOf(Category.BOOKS, Category.TOOLS),
        district = "Miraflores",
        status = status,
        owner = OwnerSummary("carlos", "Carlos R.", "Miraflores"),
        createdAt = Instant.parse("2026-11-20T15:00:00Z"),
        updatedAt = Instant.parse("2026-11-20T15:00:00Z"),
        description = "Aro 26, 21 cambios.",
        distanceKm = 1.2,
    )

    private fun show(state: DetailUiState, actions: DetailActions = DetailActions()) {
        compose.setContent {
            MinkaTheme {
                ListingDetailContent(state, actions, SnackbarHostState(), now)
            }
        }
    }

    private fun loaded(
        owner: Boolean,
        status: ListingStatus = ListingStatus.ACTIVE,
        confirmingDelete: Boolean = false,
    ) = DetailUiState(
        phase = DetailPhase.Loaded,
        listing = listing(status),
        isOwner = owner,
        confirmingDelete = confirmingDelete,
    )

    @Test
    fun visitorSeesTheListingAndOpensTheOwner() {
        var opened: String? = null
        show(loaded(owner = false), DetailActions(onOpenOwner = { opened = it }))

        compose.onNodeWithText("Bicicleta de montaña aro 26").assertIsDisplayed()
        compose.onNodeWithText("Miraflores · 1,2 km · publicada hace 2 días").assertIsDisplayed()
        compose.onNodeWithText("Busca a cambio").assertIsDisplayed()
        compose.onAllNodesWithText("Pausar").assertCountEquals(0)
        compose.onNodeWithText("Carlos R.").performClick()
        assertEquals("carlos", opened)
    }

    @Test
    fun theMoreMenuOnlyOffersHide() {
        var hidden = false
        show(loaded(owner = false), DetailActions(onHide = { hidden = true }))

        compose.onNodeWithContentDescription("Más opciones").performClick()
        compose.onAllNodesWithText("Reportar publicación").assertCountEquals(0)
        compose.onNodeWithText("Ocultar").performClick()
        assertEquals(true, hidden)
    }

    @Test
    fun ownerToolbarPausesAndAsksBeforeDeleting() {
        var paused = false
        var deleteRequested = false
        show(loaded(owner = true), DetailActions(onPause = { paused = true }, onDelete = { deleteRequested = true }))

        compose.onNodeWithText("Pausar").performClick()
        compose.onNodeWithText("Borrar").performClick()
        assertEquals(true, paused)
        assertEquals(true, deleteRequested)
        compose.onAllNodesWithText("Reanudar").assertCountEquals(0)
    }

    @Test
    fun pausedOwnerViewShowsTheTagAndResume() {
        var resumed = false
        show(loaded(owner = true, status = ListingStatus.PAUSED), DetailActions(onResume = { resumed = true }))

        compose.onNodeWithText("PAUSADA").assertIsDisplayed()
        compose.onNodeWithText("Reanudar").performClick()
        assertEquals(true, resumed)
    }

    @Test
    fun deleteConfirmationOffersPausingInstead() {
        var paused = false
        var confirmed = false
        show(
            loaded(owner = true, confirmingDelete = true),
            DetailActions(onPause = { paused = true }, onConfirmDelete = { confirmed = true }),
        )

        compose.onNodeWithText("¿Borrar esta publicación?").assertIsDisplayed()
        compose.onNodeWithText("Pausar en su lugar").performClick()
        assertEquals(true, paused)
        assertEquals(false, confirmed)
    }

    @Test
    fun loadingSkeletonKeepsTheBackButton() {
        var back = false
        show(DetailUiState(), DetailActions(onBack = { back = true }))

        compose.onNodeWithContentDescription("Volver").performClick()
        assertEquals(true, back)
    }

    @Test
    fun unavailableListingLeadsToSimilarObjects() {
        var searched = false
        show(DetailUiState(phase = DetailPhase.Unavailable), DetailActions(onFindSimilar = { searched = true }))

        compose.onNodeWithText("Ya no disponible").assertIsDisplayed()
        compose.onNodeWithText("Ver similares cerca").performClick()
        assertEquals(true, searched)
    }
}
