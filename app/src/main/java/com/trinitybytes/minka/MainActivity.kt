package com.trinitybytes.minka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.features.onboarding.presentation.SplashViewModel
import com.trinitybytes.minka.navigation.MinkaNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val launchViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Figma 1.01 (design A1): the system splash stays up while the session is restored.
        installSplashScreen().setKeepOnScreenCondition { launchViewModel.keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        val lightBars = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent {
            MinkaTheme {
                val launch by launchViewModel.uiState.collectAsStateWithLifecycle()
                // Composed right away so a recreated activity gets its back stack back.
                MinkaNavHost(launch)
            }
        }
    }
}
