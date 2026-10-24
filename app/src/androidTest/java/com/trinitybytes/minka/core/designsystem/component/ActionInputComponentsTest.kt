package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionInputComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun buttonRunsItsActionWhenEnabled() {
        var clicks = 0
        compose.setContent {
            MinkaTheme { MinkaButton(text = "Proponer trueque", onClick = { clicks++ }) }
        }

        compose.onNodeWithText("Proponer trueque").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun disabledButtonIsNotEnabled() {
        compose.setContent {
            MinkaTheme {
                MinkaButton(text = "Eliminar", onClick = {}, style = MinkaButtonStyle.Destructive, enabled = false)
            }
        }

        compose.onNodeWithText("Eliminar").assertIsNotEnabled()
    }

    @Test
    fun textFieldShowsLabelAndSupportingTextAndReportsInput() {
        var value by mutableStateOf("")
        compose.setContent {
            MinkaTheme {
                MinkaTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Título de la publicación",
                    supportingText = "Texto de ayuda",
                )
            }
        }

        compose.onNodeWithText("Título de la publicación").assertIsDisplayed()
        compose.onNodeWithText("Texto de ayuda").assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)).performTextInput("Bici")

        assertEquals("Bici", value)
    }

    @Test
    fun textFieldInErrorExposesTheErrorMessage() {
        compose.setContent {
            MinkaTheme {
                MinkaTextField(
                    value = "x",
                    onValueChange = {},
                    label = "Correo",
                    supportingText = "Ingresa un correo válido",
                    isError = true,
                )
            }
        }

        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Ingresa un correo válido"))
    }

    @Test
    fun checkboxRowTogglesFromItsLabel() {
        compose.setContent {
            MinkaTheme {
                var checked by remember { mutableStateOf(false) }
                MinkaCheckboxRow(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    label = "Entiendo que esta acción no se puede deshacer",
                )
            }
        }

        val row = compose.onNodeWithText("Entiendo que esta acción no se puede deshacer", useUnmergedTree = false)
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
        row.performClick()
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun strengthMeterNamesTheLevel() {
        compose.setContent { MinkaTheme { MinkaStrengthMeter(level = StrengthLevel.Strong) } }

        compose.onNodeWithText("Fuerte").assertIsDisplayed()
    }
}
