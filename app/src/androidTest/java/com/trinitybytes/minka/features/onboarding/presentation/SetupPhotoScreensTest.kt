package com.trinitybytes.minka.features.onboarding.presentation

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Layout of the photo setup steps, Figma 1.20 and 1.21 (design A2, A5). */
@RunWith(AndroidJUnit4::class)
class SetupPhotoScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setNotifications(configuration: DeviceConfigurationOverride, onEnable: () -> Unit = {}) {
        compose.setContent {
            MinkaTheme {
                DeviceConfigurationOverride(configuration) { NotificationsContent(onEnable = onEnable, onNotNow = {}) }
            }
        }
    }

    @Test
    fun expandedWindowShowsThePhotoAndTheTextsInTwoPanes() {
        setNotifications(DeviceConfigurationOverride.ForcedSize(DpSize(1000.dp, 700.dp)))

        val actionLeft = compose.onNodeWithText("Activar notificaciones").getBoundsInRoot().left
        assertTrue(actionLeft > compose.onRoot().getBoundsInRoot().width * 0.4f)
    }

    @Test
    fun actionsStayReachableInPhoneLandscapeAtDoubleFontScale() {
        var enabled = 0
        setNotifications(
            DeviceConfigurationOverride.FontScale(2f) then DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 360.dp)),
            onEnable = { enabled++ },
        )

        compose.onNodeWithText("Activar notificaciones").performScrollTo().assertIsDisplayed().performClick()

        assertEquals(1, enabled)
    }

    @Test
    fun allSetKeepsItsActionReachableAtDoubleFontScale() {
        compose.setContent {
            MinkaTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f) then DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 360.dp)),
                ) {
                    AllSetContent(AllSetUiState(firstName = "Ana", districtName = "Miraflores"), onExplore = {})
                }
            }
        }

        compose.onNodeWithText("Explorar Mink'a").performScrollTo().assertIsDisplayed()
    }
}
