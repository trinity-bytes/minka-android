package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import com.trinitybytes.minka.features.items.domain.SavedDraft
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyListingsContentTest {
    @get:Rule val compose = createComposeRule()

    private fun listing(id: String, title: String, status: ListingStatus = ListingStatus.ACTIVE) = Listing(
        id = id,
        cover = "",
        morePhotos = emptyList(),
        title = title,
        category = Category.HOME,
        condition = Condition.GOOD,
        wants = listOf(Category.BOOKS),
        district = "Miraflores",
        status = status,
        owner = OwnerSummary("ana", "Ana Quispe", "Miraflores"),
        createdAt = Instant.now().minusSeconds(2 * 86_400L),
        updatedAt = Instant.now().minusSeconds(2 * 86_400L),
    )

    private fun show(state: MyListingsUiState, actions: MyListingsActions = MyListingsActions()) {
        compose.setContent {
            MinkaTheme { MyListingsContent(state, SnackbarHostState(), actions) }
        }
    }

    @Test fun chipsOfTheStatusFilterAreShownAndTheCurrentOneIsSelected() {
        show(MyListingsUiState(loading = false))
        compose.onNodeWithText("Activas").assertIsSelected()
        compose.onNodeWithText("Pausadas").assertIsDisplayed()
        compose.onNodeWithText("Borradores").assertIsDisplayed()
    }

    @Test fun choosingAChipAsksForThatFilter() {
        var chosen: MyListingsFilter? = null
        show(MyListingsUiState(loading = false), MyListingsActions(onSelectFilter = { chosen = it }))
        compose.onNodeWithText("Borradores").performClick()
        compose.runOnIdle { assertEquals(MyListingsFilter.DRAFTS, chosen) }
    }

    @Test fun draftRowContinuesTheDraft() {
        var continued = ""
        val draft = SavedDraft("d1", ListingDraft(title = "Juego de platos"), Instant.now())
        show(
            MyListingsUiState(loading = false, filter = MyListingsFilter.DRAFTS, drafts = listOf(draft)),
            MyListingsActions(onContinueDraft = { continued = it }),
        )
        compose.onNodeWithText("Borrador · paso 1 de 3").assertIsDisplayed()
        compose.onNodeWithText("Continuar").performClick()
        compose.runOnIdle { assertEquals("d1", continued) }
    }

    @Test fun overflowMenuPausesAnActiveListingAndHidesEditar() {
        var paused = ""
        show(
            MyListingsUiState(loading = false, active = listOf(listing("a", "Bicicleta"))),
            MyListingsActions(onPause = { paused = it }),
        )
        compose.onNodeWithContentDescription("Más opciones de Bicicleta").performClick()
        compose.onNodeWithText("Editar").assertDoesNotExist()
        compose.onNodeWithText("Borrar").assertIsDisplayed()
        compose.onNodeWithText("Pausar").performClick()
        compose.runOnIdle { assertEquals("a", paused) }
    }

    @Test fun pausedListingOffersReanudar() {
        var resumed = ""
        show(
            MyListingsUiState(
                loading = false,
                filter = MyListingsFilter.PAUSED,
                paused = listOf(listing("p", "Mesa de centro", ListingStatus.PAUSED)),
            ),
            MyListingsActions(onResume = { resumed = it }),
        )
        compose.onNodeWithContentDescription("Más opciones de Mesa de centro").performClick()
        compose.onNodeWithText("Reanudar").performClick()
        compose.runOnIdle { assertEquals("p", resumed) }
    }

    @Test fun borrarAsksForConfirmationBeforeDeleting() {
        var requested = ""
        var confirmed = false
        val actions = MyListingsActions(onRequestDelete = { requested = it }, onConfirmDelete = { confirmed = true })
        show(MyListingsUiState(loading = false, active = listOf(listing("a", "Bicicleta"))), actions)
        compose.onNodeWithContentDescription("Más opciones de Bicicleta").performClick()
        compose.onNodeWithText("Borrar").performClick()
        compose.runOnIdle {
            assertEquals("a", requested)
            assertEquals(false, confirmed)
        }
    }

    @Test fun confirmationDialogUsesTheDeleteCopyOf408() {
        var confirmed = false
        show(
            MyListingsUiState(loading = false, active = listOf(listing("a", "Bicicleta")), pendingDelete = "a"),
            MyListingsActions(onConfirmDelete = { confirmed = true }),
        )
        compose.onNodeWithText("¿Borrar esta publicación?").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
        compose.onNodeWithText("Borrar", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(true, confirmed) }
    }

    @Test fun tappingARowOpensItsListingOnCompactWidth() {
        var opened = ""
        show(
            MyListingsUiState(loading = false, active = listOf(listing("a", "Bicicleta"))),
            MyListingsActions(onOpenListing = { opened = it }),
        )
        compose.onNodeWithText("Bicicleta").performClick()
        compose.runOnIdle { assertEquals("a", opened) }
    }

    @Test fun emptyDraftsExplainWhereDraftsComeFrom() {
        show(MyListingsUiState(loading = false, filter = MyListingsFilter.DRAFTS))
        compose.onNodeWithText("No tienes borradores").assertIsDisplayed()
    }
}
