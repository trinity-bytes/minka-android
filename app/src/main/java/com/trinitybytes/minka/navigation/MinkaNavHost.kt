package com.trinitybytes.minka.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.trinitybytes.minka.features.auth.presentation.PasswordRecoveryScreen
import com.trinitybytes.minka.features.auth.presentation.RegisterPlaceholderScreen
import com.trinitybytes.minka.features.auth.presentation.SignInScreen
import com.trinitybytes.minka.features.home.presentation.HomeScreen
import com.trinitybytes.minka.features.onboarding.presentation.SplashDestination
import com.trinitybytes.minka.features.onboarding.presentation.SplashScreen
import com.trinitybytes.minka.features.onboarding.presentation.WelcomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object WelcomeRoute

/** Sign-in (Figma 1.05–1.09); [withGoogle] opens the Google account picker right away. */
@Serializable
data class SignInRoute(val withGoogle: Boolean = false)

/** Password recovery (Figma 1.10–1.11). */
@Serializable
data object PasswordRecoveryRoute

/** Registration (Figma 1.12–1.15). */
@Serializable
data object RegisterRoute

@Serializable
data object HomeRoute

@Composable
fun MinkaNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SplashRoute) {
        composable<SplashRoute> {
            SplashScreen(
                onNavigate = { destination ->
                    val route: Any = when (destination) {
                        SplashDestination.Home -> HomeRoute
                        SplashDestination.Welcome -> WelcomeRoute
                    }
                    navController.navigate(route) {
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<WelcomeRoute> {
            WelcomeScreen(
                onCreateAccount = { navController.navigate(RegisterRoute) },
                onSignIn = { navController.navigate(SignInRoute()) },
                onContinueWithGoogle = { navController.navigate(SignInRoute(withGoogle = true)) },
            )
        }
        composable<SignInRoute> { entry ->
            SignInScreen(
                withGoogle = entry.toRoute<SignInRoute>().withGoogle,
                onBack = { navController.popBackStack() },
                onSignedIn = {
                    // A signed-in user never goes back to the welcome or sign-in screens.
                    navController.navigate(HomeRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onRecoverPassword = { navController.navigate(PasswordRecoveryRoute) },
                onRegister = { navController.navigate(RegisterRoute) },
            )
        }
        composable<PasswordRecoveryRoute> {
            // Back, from either 1.10 or 1.11, returns to sign-in, which is right below on the stack.
            PasswordRecoveryScreen(onBack = { navController.popBackStack() })
        }
        composable<RegisterRoute> { RegisterPlaceholderScreen() }
        composable<HomeRoute> { HomeScreen() }
    }
}
