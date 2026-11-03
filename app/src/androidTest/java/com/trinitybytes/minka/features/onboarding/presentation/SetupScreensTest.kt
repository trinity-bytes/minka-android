package com.trinitybytes.minka.features.onboarding.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.Interest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val districts = listOf(District("miraflores", "Miraflores"), District("san-isidro", "San Isidro"))

    @Test
    fun continuarWaitsForADistrict() {
        compose.setContent {
            var state by remember { mutableStateOf(DistrictUiState(districts = districts, isLoading = false)) }
            MinkaTheme {
                DistrictContent(
                    uiState = state,
                    onBack = null,
                    onUseMyLocation = {},
                    onQueryChange = { state = state.copy(query = it) },
                    onDistrictSelected = { state = state.copy(selectedId = it) },
                    onContinue = {},
                    onOpenSettings = {},
                    onMessageShown = { state = state.copy(message = null) },
                )
            }
        }
        compose.onNodeWithText("Continuar").assertIsNotEnabled()

        compose.onNodeWithText("San Isidro").performClick()

        compose.onNodeWithText("San Isidro").assertIsSelected()
        compose.onNodeWithText("Continuar").assertIsEnabled()
    }

    @Test
    fun deniedLocationOffersTheSettings() {
        var settingsOpened = 0
        compose.setContent {
            MinkaTheme {
                DistrictContent(
                    uiState = DistrictUiState(districts = districts, isLoading = false, message = SetupMessage.LocationDenied),
                    onBack = null,
                    onUseMyLocation = {},
                    onQueryChange = {},
                    onDistrictSelected = {},
                    onContinue = {},
                    onOpenSettings = { settingsOpened++ },
                    onMessageShown = {},
                )
            }
        }
        compose.onNodeWithText("Sin permiso de ubicación. Elige tu distrito.").assertExists()

        compose.onNodeWithText("Ajustes").performClick()
        compose.waitForIdle()

        assertEquals(1, settingsOpened)
    }

    @Test
    fun interestsCountTheChosenChipsAndCanBeSkipped() {
        var skipped = 0
        compose.setContent {
            var state by remember {
                mutableStateOf(InterestsUiState(interests = listOf(Interest("libros", "Libros"), Interest("arte", "Arte")), isLoading = false))
            }
            MinkaTheme {
                InterestsContent(
                    uiState = state,
                    onBack = {},
                    onInterestToggled = { id -> state = state.copy(selectedIds = state.selectedIds + id) },
                    onSkip = { skipped++ },
                    onContinue = {},
                    onMessageShown = {},
                )
            }
        }
        compose.onNodeWithText("Continuar").assertIsNotEnabled()

        compose.onNodeWithText("Libros").performClick()
        compose.onNodeWithText("1 seleccionado").assertExists()
        compose.onNodeWithText("Continuar").assertIsEnabled()

        compose.onNodeWithText("Omitir").performClick()
        assertEquals(1, skipped)
    }

    @Test
    fun allSetGreetsAndExplores() {
        var explored = 0
        compose.setContent {
            MinkaTheme { AllSetContent(AllSetUiState(firstName = "Ana", districtName = "Miraflores"), onExplore = { explored++ }) }
        }
        compose.onNodeWithText("¡Todo listo, Ana!").assertExists()
        compose.onNodeWithText("Ya puedes publicar tu primer objeto o buscar trueques en Miraflores.").assertExists()

        compose.onNodeWithText("Explorar Mink'a").performClick()

        assertEquals(1, explored)
    }

    @Test
    fun notificationsOfferBothAnswers() {
        val taps = mutableListOf<String>()
        compose.setContent {
            MinkaTheme { NotificationsContent(onEnable = { taps += "enable" }, onNotNow = { taps += "later" }) }
        }
        compose.onNodeWithText("Activar notificaciones").performClick()
        compose.onNodeWithText("Ahora no").performClick()

        assertEquals(listOf("enable", "later"), taps)
    }
}
