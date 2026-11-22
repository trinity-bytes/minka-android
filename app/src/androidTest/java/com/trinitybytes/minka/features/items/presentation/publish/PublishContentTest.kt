package com.trinitybytes.minka.features.items.presentation.publish

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.DraftError
import com.trinitybytes.minka.features.items.domain.ListingDraft
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PublishContentTest {
    @get:Rule val compose = createComposeRule()

    private val photos = listOf("file:///none/1.jpg", "file:///none/2.jpg")

    private fun show(state: PublishUiState, actions: PublishActions = PublishActions()) {
        compose.setContent {
            MinkaTheme {
                PublishContent(state = state, actions = actions, snackbarHostState = SnackbarHostState())
            }
        }
    }

    @Test
    fun continuarIsEnabledOnAnEmptyFormAndAsksToValidate() {
        var continued = 0
        show(PublishUiState(), PublishActions(onContinue = { continued++ }))
        compose.onNodeWithText("Continuar").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, continued) }
    }

    @Test
    fun missingFieldsShowTheirMessages() {
        val errors = setOf(
            DraftError.PHOTO_REQUIRED,
            DraftError.TITLE_REQUIRED,
            DraftError.CATEGORY_REQUIRED,
            DraftError.CONDITION_REQUIRED,
        )
        show(PublishUiState(errors = errors))
        compose.onNodeWithText("Agrega al menos una foto").assertIsDisplayed()
        compose.onNodeWithText("Agrega un título").assertIsDisplayed()
        compose.onNodeWithText("Elige una categoría").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Elige el estado").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun everyPhotoHasItsOwnRemoveButtonAndTappingOpensTheActions() {
        val removed = mutableListOf<Int>()
        val tapped = mutableListOf<Int>()
        show(
            PublishUiState(draft = ListingDraft(photos = photos)),
            PublishActions(onPhotoRemoved = { removed += it }, onPhotoTapped = { tapped += it }),
        )
        compose.onNodeWithContentDescription("Quitar foto 2").performClick()
        compose.onNodeWithContentDescription("Foto 1, portada").performClick()
        compose.runOnIdle {
            assertEquals(listOf(1), removed)
            assertEquals(listOf(0), tapped)
        }
    }

    @Test
    fun guardarBorradorSitsNextToTheMainAction() {
        var saved = 0
        show(PublishUiState(page = PublishPage.Wants), PublishActions(onSaveDraft = { saved++ }))
        compose.onNodeWithText("Guardar borrador").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, saved) }
    }

    @Test
    fun theFirstStepOffersCloseAndLaterStepsOfferBack() {
        show(PublishUiState(page = PublishPage.Basics))
        compose.onNodeWithContentDescription("Cerrar").assertIsDisplayed()
    }

    @Test
    fun laterStepsShowTheBackArrow() {
        show(PublishUiState(page = PublishPage.Wants))
        compose.onNodeWithContentDescription("Volver").assertIsDisplayed()
    }

    @Test
    fun photoSourceSheetOffersCameraAndGalleryWithoutAPermissionPrompt() {
        var camera = 0
        var gallery = 0
        show(
            PublishUiState(overlay = PublishOverlay.PhotoSource),
            PublishActions(onTakePhoto = { camera++ }, onPickFromGallery = { gallery++ }),
        )
        compose.onNodeWithText("Tomar foto").assertIsDisplayed().performClick()
        compose.onNodeWithText("Elegir de la galería").performClick()
        compose.runOnIdle {
            assertEquals(1, camera)
            assertEquals(1, gallery)
        }
    }

    @Test
    fun exitDialogOffersSaveAndDiscard() {
        var discarded = 0
        var saved = 0
        show(
            PublishUiState(overlay = PublishOverlay.ExitConfirm),
            PublishActions(onExitDiscard = { discarded++ }, onExitSave = { saved++ }),
        )
        compose.onNodeWithText("¿Guardar como borrador?").assertIsDisplayed()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Guardar").performClick()
        compose.runOnIdle {
            assertEquals(1, discarded)
            assertEquals(1, saved)
        }
    }

    @Test
    fun reviewShowsAgregarForAnEmptyOptionalRow() {
        val draft = ListingDraft(
            photos = photos,
            title = "Juego de platos",
            category = Category.HOME,
            condition = Condition.GOOD,
            wants = listOf(Category.BOOKS),
            district = "Miraflores",
            description = "Seis platos",
        )
        show(PublishUiState(page = PublishPage.Review, draft = draft))
        compose.onNodeWithText("Publicar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Agregar Valor estimado (opcional)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Editar Fotos").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun wantsPageHidesEcoPuntosAndOffersTheSevenCategories() {
        show(PublishUiState(page = PublishPage.Wants))
        listOf("Libros", "Ropa", "Hogar", "Herramientas", "Plantas", "Servicios", "Lo que sea").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        compose.onNodeWithText("También acepto Eco-Puntos").assertDoesNotExist()
    }
}
