package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInRecompositionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun countdownTickDoesNotRecomposeForm() {
        val state = MutableStateFlow(SignInUiState(contact = "Ana", lockSecondsLeft = 60))
        var formCompositions = 0
        var dialogCompositions = 0
        compose.setContent {
            SignInStateCollector(state, lock = { countdownState ->
                SideEffect { dialogCompositions++ }
                SignInCountdownText(countdownState)
            }) {
                SideEffect { formCompositions++ }
                Text(it.contact)
            }
        }
        compose.waitForIdle()
        val before = formCompositions
        val dialogBefore = dialogCompositions
        compose.runOnIdle { state.value = state.value.copy(lockSecondsLeft = 59) }
        compose.onNodeWithText("0:59", substring = true).assertExists()
        compose.runOnIdle {
            assertEquals(before, formCompositions)
            assertEquals(dialogBefore, dialogCompositions)
        }
    }
}
