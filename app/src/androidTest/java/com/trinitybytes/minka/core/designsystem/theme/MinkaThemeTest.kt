package com.trinitybytes.minka.core.designsystem.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MinkaThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lightColorsAreUsedInBothSystemModes() {
        val observed = arrayOfNulls<Pair<Color, Color>>(2)
        compose.setContent {
            val current = LocalConfiguration.current
            listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES).forEachIndexed { index, mode ->
                val configuration = Configuration(current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
                }
                CompositionLocalProvider(LocalConfiguration provides configuration) {
                    MinkaTheme {
                        val colors = MaterialTheme.colorScheme.surface to MinkaTheme.colors.mint
                        SideEffect { observed[index] = colors }
                    }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(listOf(LightColorScheme.surface to LightMinkaColors.mint, LightColorScheme.surface to LightMinkaColors.mint), observed.toList())
        }
    }
}
