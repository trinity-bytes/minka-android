package com.trinitybytes.minka.features.items.presentation.publish

import androidx.lifecycle.SavedStateHandle
import com.trinitybytes.minka.features.items.application.GetMyDrafts
import com.trinitybytes.minka.features.items.application.GetMyListings
import com.trinitybytes.minka.features.items.application.PublishListing
import com.trinitybytes.minka.features.items.application.ResumeListingDraft
import com.trinitybytes.minka.features.items.application.SaveListingDraft
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.DraftError
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.MAX_PHOTOS
import com.trinitybytes.minka.features.items.infrastructure.ItemsHarness
import com.trinitybytes.minka.features.items.infrastructure.value
import com.trinitybytes.minka.features.onboarding.application.GetDistricts
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
import com.trinitybytes.minka.navigation.PublishRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublishViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val harness = ItemsHarness()
    private val handle = SavedStateHandle()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        route: PublishRoute = PublishRoute(),
        state: SavedStateHandle = handle,
    ) = PublishViewModel(
        saveDraft = SaveListingDraft(harness.repository),
        publishListing = PublishListing(harness.repository),
        resumeDraft = ResumeListingDraft(harness.repository),
        getDistricts = GetDistricts(harness.profiles),
        getSetupSummary = GetSetupSummary(harness.sessions, harness.profiles),
        savedStateHandle = state,
        route = route,
    )

    private val PublishViewModel.state get() = uiState.value

    /** The mock API answers on its own threads, so tests wait on the state instead of the test scheduler. */
    private suspend fun PublishViewModel.await(until: (PublishUiState) -> Boolean) = uiState.first(until)

    private suspend fun TestScope.ready(route: PublishRoute = PublishRoute()): PublishViewModel {
        val model = viewModel(route)
        advanceUntilIdle()
        model.await { it.phase != PublishPhase.Loading }
        return model
    }

    private fun PublishViewModel.fillBasics() {
        onPhotosPicked(listOf("file:///photos/1.jpg", "file:///photos/2.jpg"))
        onTitleChange("Juego de platos")
        onCategoryChosen(Category.HOME)
        onConditionChosen(Condition.GOOD)
    }

    @Test
    fun `continuar on an empty step 1 stays and shows what is missing`() = runTest(dispatcher) {
        val model = ready()
        model.onContinue()
        assertEquals(PublishPage.Basics, model.state.page)
        assertEquals(
            setOf(
                DraftError.PHOTO_REQUIRED,
                DraftError.TITLE_REQUIRED,
                DraftError.CATEGORY_REQUIRED,
                DraftError.CONDITION_REQUIRED,
            ),
            model.state.errors,
        )
    }

    @Test
    fun `errors disappear as each field is fixed and a complete step 1 moves to step 2`() = runTest(dispatcher) {
        val model = ready()
        model.onContinue()
        model.onTitleChange("Platos")
        assertFalse(DraftError.TITLE_REQUIRED in model.state.errors)
        model.onConditionChosen(Condition.NEW)
        assertFalse(DraftError.CONDITION_REQUIRED in model.state.errors)
        model.onCategoryChosen(Category.BOOKS)
        model.onPhotosPicked(listOf("file:///a.jpg"))
        assertTrue(model.state.errors.isEmpty())
        model.onContinue()
        assertEquals(PublishPage.Wants, model.state.page)
        assertTrue(model.state.errors.isEmpty())
    }

    @Test
    fun `step 2 needs one wanted category and a district before the review`() = runTest(dispatcher) {
        val model = ready()
        model.fillBasics()
        model.onContinue()
        model.onContinue()
        assertEquals(PublishPage.Wants, model.state.page)
        assertEquals(setOf(DraftError.EXCHANGE_REQUIRED), model.state.errors)
        model.onWantToggled(Category.BOOKS)
        model.onContinue()
        assertEquals(PublishPage.Location, model.state.page)
        model.onContinue()
        assertEquals(PublishPage.Location, model.state.page)
        assertEquals(setOf(DraftError.DISTRICT_REQUIRED), model.state.errors)
        model.onDistrictChosen("Miraflores")
        model.onContinue()
        assertEquals(PublishPage.Review, model.state.page)
    }

    @Test
    fun `lo que sea excludes the other wanted categories and the other way round`() = runTest(dispatcher) {
        val model = ready()
        model.onWantToggled(Category.BOOKS)
        model.onWantToggled(Category.TOOLS)
        assertEquals(listOf(Category.BOOKS, Category.TOOLS), model.state.draft.wants)
        model.onWantToggled(Category.ANY)
        assertEquals(listOf(Category.ANY), model.state.draft.wants)
        model.onWantToggled(Category.PLANTS)
        assertEquals(listOf(Category.PLANTS), model.state.draft.wants)
        model.onWantToggled(Category.PLANTS)
        assertTrue(model.state.draft.wants.isEmpty())
    }

    @Test
    fun `a new flow starts in the district of the profile`() = runTest(dispatcher) {
        harness.profiles.saveDistrict("barranco")
        val model = ready()
        model.await { it.draft.district.isNotEmpty() }
        assertEquals("Barranco", model.state.draft.district)
        model.onClose()
        // The prefilled district alone is not unsaved work.
        assertEquals(PublishEvent.Close, model.events.first())
    }

    @Test
    fun `back walks the pages and closes from step 1 only when nothing was typed`() = runTest(dispatcher) {
        val model = ready()
        model.fillBasics()
        model.onContinue()
        model.onWantToggled(Category.BOOKS)
        model.onContinue()
        model.onDistrictChosen("Miraflores")
        model.onContinue()
        assertEquals(PublishPage.Review, model.state.page)
        model.onBack()
        assertEquals(PublishPage.Location, model.state.page)
        model.onBack()
        assertEquals(PublishPage.Wants, model.state.page)
        model.onBack()
        assertEquals(PublishPage.Basics, model.state.page)
        model.onBack()
        assertEquals(PublishOverlay.ExitConfirm, model.state.overlay)
    }

    @Test
    fun `closing an untouched flow leaves right away`() = runTest(dispatcher) {
        val model = ready()
        model.onClose()
        assertEquals(PublishEvent.Close, model.events.first())
    }

    @Test
    fun `closing with data offers guardar and descartar, and descartar keeps nothing`() = runTest(dispatcher) {
        val model = ready()
        model.onTitleChange("Platos")
        model.onClose()
        assertEquals(PublishOverlay.ExitConfirm, model.state.overlay)
        model.onExitDiscard()
        assertEquals(PublishEvent.Close, model.events.first())
        assertTrue(GetMyDrafts(harness.repository)().value().isEmpty())
    }

    @Test
    fun `guardar in the exit dialog saves the draft as it is and closes`() = runTest(dispatcher) {
        val model = ready()
        model.onTitleChange("Platos")
        model.onClose()
        model.onExitSave()
        assertEquals(PublishEvent.Close, model.events.first())
        val drafts = GetMyDrafts(harness.repository)().value()
        assertEquals(listOf("Platos"), drafts.map { it.draft.title })
    }

    @Test
    fun `guardar borrador keeps editing and the next save updates the same draft`() = runTest(dispatcher) {
        val model = ready()
        model.onTitleChange("Platos")
        model.onSaveDraft()
        model.await { it.message == PublishMessage.DraftSaved }
        assertEquals(PublishPage.Basics, model.state.page)
        model.onTitleChange("Platos azules")
        model.onMessageShown()
        model.onSaveDraft()
        model.await { it.message == PublishMessage.DraftSaved }
        val drafts = GetMyDrafts(harness.repository)().value()
        assertEquals(listOf("Platos azules"), drafts.map { it.draft.title })
    }

    @Test
    fun `photos stop at the maximum, the first one is the cover and it can change or go`() = runTest(dispatcher) {
        val model = ready()
        model.onPhotosPicked((1..8).map { "file:///$it.jpg" })
        assertEquals(MAX_PHOTOS, model.state.draft.photos.size)
        assertEquals("file:///1.jpg", model.state.draft.photos.first())
        model.onMakeCover(2)
        assertEquals(listOf("file:///3.jpg", "file:///1.jpg", "file:///2.jpg"), model.state.draft.photos.take(3))
        model.onPhotoRemoved(0)
        assertEquals("file:///1.jpg", model.state.draft.photos.first())
        assertEquals(MAX_PHOTOS - 1, model.state.draft.photos.size)
    }

    @Test
    fun `tapping a photo opens its actions and adding opens the source sheet only below the maximum`() =
        runTest(dispatcher) {
            val model = ready()
            model.onPhotosPicked(listOf("file:///1.jpg", "file:///2.jpg"))
            model.onPhotoTapped(1)
            assertEquals(PublishOverlay.PhotoActions(1), model.state.overlay)
            model.onMakeCover(1)
            assertNull(model.state.overlay)
            model.onAddPhotosTapped()
            assertEquals(PublishOverlay.PhotoSource, model.state.overlay)
            model.onPhotosPicked((1..6).map { "file:///x$it.jpg" })
            assertNull(model.state.overlay)
            model.onAddPhotosTapped()
            assertNull(model.state.overlay)
        }

    @Test
    fun `the details sheet stores the description and the estimated value in centavos`() = runTest(dispatcher) {
        val model = ready()
        model.onDetailsDone(description = " Seis platos ", estimatedSoles = "120")
        assertEquals("Seis platos", model.state.draft.description)
        assertEquals(12_000L, model.state.draft.estimatedValue)
        model.onDetailsDone(description = "", estimatedSoles = "")
        assertNull(model.state.draft.description)
        assertNull(model.state.draft.estimatedValue)
        model.onDetailsDone(description = "x", estimatedSoles = "45,50")
        assertEquals(4_550L, model.state.draft.estimatedValue)
    }

    @Test
    fun `publicar publishes the draft and shows the published listing`() = runTest(dispatcher) {
        val model = ready()
        model.fillBasics()
        model.onWantToggled(Category.BOOKS)
        model.onDistrictChosen("Miraflores")
        model.onEdit(PublishPage.Review)
        model.onPublish()
        assertEquals(PublishPhase.Publishing, model.state.phase)
        model.await { it.phase == PublishPhase.Published }
        val listing = model.state.published
        assertNotNull(listing)
        assertEquals("Juego de platos", listing!!.title)
        val mine = GetMyListings(harness.repository)(ListingStatus.ACTIVE).value().value
        assertTrue(mine.any { it.id == listing.id })
        assertTrue(GetMyDrafts(harness.repository)().value().isEmpty())
    }

    @Test
    fun `publicar with a missing step jumps to the first page that needs work`() = runTest(dispatcher) {
        val model = ready()
        model.fillBasics()
        model.onEdit(PublishPage.Review)
        model.onPublish()
        assertEquals(PublishPhase.Editing, model.state.phase)
        assertEquals(PublishPage.Wants, model.state.page)
        assertEquals(setOf(DraftError.EXCHANGE_REQUIRED), model.state.errors)
    }

    @Test
    fun `a connection loss while publishing keeps everything as a draft and a retry succeeds`() = runTest(dispatcher) {
        val model = ready()
        model.fillBasics()
        model.onWantToggled(Category.BOOKS)
        model.onDistrictChosen("Miraflores")
        model.onEdit(PublishPage.Review)
        // The draft is saved first, then the connection drops before publishing.
        model.onSaveDraft()
        model.await { it.message == PublishMessage.DraftSaved }
        harness.offline = true
        model.onPublish()
        model.await { it.phase != PublishPhase.Publishing }
        assertEquals(PublishPhase.Editing, model.state.phase)
        assertEquals(PublishPage.Review, model.state.page)
        assertEquals(PublishMessage.PublishFailed, model.state.message)
        harness.offline = false
        model.onMessageShown()
        model.onPublish()
        model.await { it.phase == PublishPhase.Published }
        assertTrue(GetMyDrafts(harness.repository)().value().isEmpty())
    }

    @Test
    fun `a draft id resumes the saved draft and saves back to the same id`() = runTest(dispatcher) {
        val saved = SaveListingDraft(harness.repository)(null, ItemsHarness.completeDraft("Mesa")).value()
        val model = viewModel(PublishRoute(saved.id))
        assertEquals(PublishPhase.Loading, model.state.phase)
        model.await { it.phase != PublishPhase.Loading }
        assertEquals(PublishPhase.Editing, model.state.phase)
        assertEquals("Mesa", model.state.draft.title)
        model.onTitleChange("Mesa grande")
        model.onSaveDraft()
        model.await { it.message == PublishMessage.DraftSaved }
        val drafts = GetMyDrafts(harness.repository)().value()
        assertEquals(listOf("Mesa grande"), drafts.map { it.draft.title })
    }

    @Test
    fun `an unknown draft id fails to load`() = runTest(dispatcher) {
        val model = viewModel(PublishRoute("missing"))
        model.await { it.phase != PublishPhase.Loading }
        assertEquals(PublishPhase.LoadFailed, model.state.phase)
    }

    @Test
    fun `publicar otro starts a clean draft`() = runTest(dispatcher) {
        val model = ready()
        model.fillBasics()
        model.onWantToggled(Category.BOOKS)
        model.onDistrictChosen("Miraflores")
        model.onEdit(PublishPage.Review)
        model.onPublish()
        model.await { it.phase == PublishPhase.Published }
        model.onPublishAnother()
        assertEquals(PublishPhase.Editing, model.state.phase)
        assertEquals(PublishPage.Basics, model.state.page)
        assertTrue(model.state.draft.photos.isEmpty())
        assertNull(model.state.published)
    }

    @Test
    fun `the draft and the page survive a recreated view model`() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val first = viewModel(state = state)
        first.fillBasics()
        first.onDetailsDone("Seis platos", "120")
        first.onContinue()
        val restored = viewModel(state = state)
        assertEquals(PublishPage.Wants, restored.state.page)
        assertEquals(first.state.draft, restored.state.draft)
        assertEquals(PublishPhase.Editing, restored.state.phase)
    }

    @Test
    fun `the district picker lists the districts once`() = runTest(dispatcher) {
        val model = ready()
        model.onDistrictRowTapped()
        model.await { it.districts != null }
        assertEquals(PublishOverlay.District, model.state.overlay)
        assertTrue(model.state.districts.orEmpty().any { it.name == "Miraflores" })
        model.onDistrictChosen("San Isidro")
        assertNull(model.state.overlay)
        assertEquals("San Isidro", model.state.draft.district)
    }

    @Test
    fun `the meeting point is optional and an empty text clears it`() = runTest(dispatcher) {
        val model = ready()
        model.onMeetingPointRowTapped()
        assertEquals(PublishOverlay.MeetingPoint, model.state.overlay)
        model.onMeetingPointDone(" Parque Kennedy ")
        assertEquals("Parque Kennedy", model.state.draft.meetingPoint)
        model.onMeetingPointDone("")
        assertNull(model.state.draft.meetingPoint)
    }

    @Test
    fun `hiding the exact location is on by default and home delivery is off`() = runTest(dispatcher) {
        val model = ready()
        assertTrue(model.state.draft.hideExactLocation)
        assertFalse(model.state.draft.homeDelivery)
        model.onHideLocationChange(false)
        model.onHomeDeliveryChange(true)
        assertFalse(model.state.draft.hideExactLocation)
        assertTrue(model.state.draft.homeDelivery)
    }
}
