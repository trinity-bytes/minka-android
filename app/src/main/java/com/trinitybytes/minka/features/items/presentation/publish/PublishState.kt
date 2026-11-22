package com.trinitybytes.minka.features.items.presentation.publish

import com.trinitybytes.minka.features.items.domain.DraftError
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.MAX_PHOTOS
import com.trinitybytes.minka.features.onboarding.domain.District

/** Total steps the progress bar counts: "Paso 1 de 3" (Figma 5.01–5.08). */
const val PUBLISH_STEPS = 3

/**
 * The screens of the form. Step 2 ("A cambio") spans two of them: what is wanted (5.06) and where
 * the exchange happens (5.07).
 */
enum class PublishPage(val step: Int) {
    /** 5.01, 5.03, 5.05: photos, title, category and "Estado". */
    Basics(1),

    /** 5.06: the "Busca:" categories. */
    Wants(2),

    /** 5.07: district, meeting point and delivery. */
    Location(2),

    /** 5.08: preview and checklist. */
    Review(3),
}

/** What the screen is doing, apart from editing the form. */
enum class PublishPhase {
    /** Resuming a draft by its id. */
    Loading,

    /** The draft could not be loaded. */
    LoadFailed,

    /** The form (5.01–5.08). */
    Editing,

    /** 5.09. */
    Publishing,

    /** 5.10. */
    Published,
}

/** A sheet or dialog laid over the form; one at a time. */
sealed interface PublishOverlay {
    /** 5.02: "Tomar foto" or "Elegir de la galería". */
    data object PhotoSource : PublishOverlay

    /** 5.03: "Hacer portada" or "Quitar" for the photo at [index]. */
    data class PhotoActions(val index: Int) : PublishOverlay

    /** 5.04: description and estimated value. */
    data object Details : PublishOverlay

    /** The category picker behind the "Categoría" field. */
    data object Category : PublishOverlay

    /** The district picker behind the 5.07 district row. */
    data object District : PublishOverlay

    /** The meeting point text behind the 5.07 row. */
    data object MeetingPoint : PublishOverlay

    /** 5.11: "¿Guardar como borrador?". */
    data object ExitConfirm : PublishOverlay
}

/** One-off feedback shown in a snackbar. */
enum class PublishMessage {
    /** "Guardar borrador" worked. */
    DraftSaved,

    /** 1.22: no connection while saving; the form stays as it was. */
    Offline,

    /** Publishing failed after the draft was saved, so nothing is lost (5.09). */
    PublishFailed,

    Unexpected,
}

sealed interface PublishEvent {
    /** Leave the flow. */
    data object Close : PublishEvent
}

/**
 * Every state of Figma 5.01–5.11. [errors] holds what the last "Continuar" found missing (5.05) and
 * follows the edits, so each message disappears as soon as its field is valid.
 */
data class PublishUiState(
    val phase: PublishPhase = PublishPhase.Editing,
    val page: PublishPage = PublishPage.Basics,
    val draft: ListingDraft = ListingDraft(),
    val errors: Set<DraftError> = emptySet(),
    val overlay: PublishOverlay? = null,
    /** Districts for the picker; null until it is first opened. */
    val districts: List<District>? = null,
    val isSaving: Boolean = false,
    val message: PublishMessage? = null,
    val published: Listing? = null,
    /** After "Editar" on the review, "Continuar" goes back to the review instead of the next page. */
    val returnToReview: Boolean = false,
) {
    val canAddPhotos: Boolean get() = draft.photos.size < MAX_PHOTOS
}
