package com.trinitybytes.minka.features.profile.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsTheUserAndOpensMisPublicacionesWithoutLaterPhaseRows() {
        var opened = 0
        compose.setContent {
            MinkaTheme {
                ProfileContent(ProfileUiState("Ana Quispe", "AQ", "Miraflores"), onOpenMyListings = { opened++ })
            }
        }
        compose.onNodeWithText("Ana Quispe").assertIsDisplayed()
        compose.onNodeWithText("Miraflores").assertIsDisplayed()
        compose.onNodeWithText("Mis intercambios").assertDoesNotExist()
        compose.onNodeWithText("Eco-Puntos y logros").assertDoesNotExist()
        compose.onNodeWithText("Mis publicaciones").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
