package com.trinitybytes.minka.features.auth.presentation

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** D7: a 1.08 countdown tick recomposes only the dialog text, never the sign-in form. */
@RunWith(AndroidJUnit4::class)
class SignInRecompositionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun aCountdownTickDoesNotRecomposeTheForm() {
        val secondsLeft = mutableStateOf<Long?>(60)
        var contentCompositions = 0
        compose.setContent {
            MinkaTheme {
                SideEffect { contentCompositions++ }
                SignInContent(
                    uiState = SignInUiState(contact = "Ana", isLocked = true),
                    onBack = {},
                    onContactChange = {},
                    onPasswordChange = {},
                    onContactFocusLost = {},
                    onSubmit = {},
                    onForgotPassword = {},
                    onGoogle = {},
                    onRegister = {},
                    onChooseGoogleAccount = {},
                    onDismissGoogle = {},
                    onDismissLock = {},
                    onMessageShown = {},
                    onRetry = {},
                    lockSecondsLeft = { secondsLeft.value },
                )
            }
        }
        compose.onNodeWithText("1:00", substring = true).assertExists()
        val before = contentCompositions

        compose.runOnIdle { secondsLeft.value = 59 }

        compose.onNodeWithText("0:59", substring = true).assertExists()
        compose.runOnIdle { assertEquals(before, contentCompositions) }
    }
}
