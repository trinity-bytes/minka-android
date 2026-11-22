package com.trinitybytes.minka.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.trinitybytes.minka.features.auth.presentation.PasswordRecoveryScreen
import com.trinitybytes.minka.features.auth.presentation.RegisterScreen
import com.trinitybytes.minka.features.auth.presentation.SignInScreen
import com.trinitybytes.minka.features.auth.presentation.VerifyCodeScreen
import com.trinitybytes.minka.features.home.presentation.HomeScreen
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import com.trinitybytes.minka.features.onboarding.presentation.AllSetScreen
import com.trinitybytes.minka.features.onboarding.presentation.DistrictScreen
import com.trinitybytes.minka.features.onboarding.presentation.InterestsScreen
import com.trinitybytes.minka.features.onboarding.presentation.NotificationsScreen
import com.trinitybytes.minka.features.onboarding.presentation.SplashDestination
import com.trinitybytes.minka.features.onboarding.presentation.WelcomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

/** Sign-in (Figma 1.05–1.09); [withGoogle] opens the Google account picker right away. */
@Serializable
data class SignInRoute(val withGoogle: Boolean = false)

/** Password recovery (Figma 1.10–1.11). */
@Serializable
data object PasswordRecoveryRoute

/** Registration data (Figma 1.12–1.13). */
@Serializable
data object RegisterRoute

/**
 * Code verification (Figma 1.14–1.15) of the registration [challengeId]. The property names are
 * the keys `VerifyCodeViewModel` reads from its SavedStateHandle.
 */
@Serializable
data class VerifyCodeRoute(
    val challengeId: String,
    val destination: String,
    val attemptsLeft: Int,
    val resendAfterSeconds: Int,
)

/** Profile setup, district (Figma 1.16–1.18). */
@Serializable
data object DistrictRoute

/** Profile setup, interests (Figma 1.19). */
@Serializable
data object InterestsRoute

/** Profile setup, notifications (Figma 1.20). */
@Serializable
data object NotificationsRoute

/** Profile setup, "Todo listo" (Figma 1.21). */
@Serializable
data object AllSetRoute

@Serializable
data object HomeRoute

/**
 * The app graph, starting at [startDestination] (from [startRoute], resolved behind the system
 * splash). User taps that navigate go through `dropUnlessResumed`: a second tap during the
 * transition is dropped, so a double back never pops the root and a double tap never pushes twice.
 */
@Composable
fun MinkaNavHost(startDestination: Any) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onCreateAccount = dropUnlessResumed { navController.navigate(RegisterRoute) },
                onSignIn = dropUnlessResumed { navController.navigate(SignInRoute()) },
                onContinueWithGoogle = dropUnlessResumed { navController.navigate(SignInRoute(withGoogle = true)) },
            )
        }
        composable<SignInRoute> { entry ->
            SignInScreen(
                withGoogle = entry.toRoute<SignInRoute>().withGoogle,
                onBack = dropUnlessResumed { navController.popBackStack() },
                onSignedIn = { pendingStep ->
                    // A signed-in user never goes back to the welcome or sign-in screens;
                    // a setup this account left pending on the device continues where it stopped (S4).
                    navController.replaceAll(pendingStep?.route() ?: HomeRoute)
                },
                onRecoverPassword = dropUnlessResumed { navController.navigate(PasswordRecoveryRoute) },
                onRegister = dropUnlessResumed { navController.navigate(RegisterRoute) },
            )
        }
        composable<PasswordRecoveryRoute> {
            // Back, from either 1.10 or 1.11, returns to sign-in, which is right below on the stack.
            PasswordRecoveryScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onCodeSent = { challenge ->
                    navController.navigate(
                        VerifyCodeRoute(
                            challengeId = challenge.id,
                            destination = challenge.destination,
                            attemptsLeft = challenge.attemptsLeft,
                            resendAfterSeconds = challenge.resendAfterSeconds,
                        ),
                    )
                },
            )
        }
        composable<VerifyCodeRoute> {
            VerifyCodeScreen(
                // Back returns to the form, which keeps what was typed.
                onBack = dropUnlessResumed { navController.popBackStack() },
                onVerified = {
                    // The account is signed in: the welcome and registration screens are left behind.
                    navController.navigate(DistrictRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                // Placeholder: there is no support channel yet.
                onContactSupport = {},
            )
        }
        composable<DistrictRoute> {
            val back = dropUnlessResumed { navController.popBackStack() }
            DistrictScreen(
                // After verification nothing is behind; a resumed setup may have nothing either.
                onBack = back.takeIf { navController.previousBackStackEntry != null },
                onChosen = { navController.navigate(InterestsRoute) },
            )
        }
        composable<InterestsRoute> {
            InterestsScreen(
                onBack = dropUnlessResumed {
                    // A setup resumed at this step (S4) has no district screen below: open it in its place.
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.replaceAll(DistrictRoute)
                    }
                },
                onDone = { navController.replaceAll(NotificationsRoute) },
            )
        }
        composable<NotificationsRoute> {
            NotificationsScreen(onDone = { navController.replaceAll(AllSetRoute) })
        }
        composable<AllSetRoute> {
            // 1.21 "lleva a Inicio": onboarding is over, nothing stays behind.
            AllSetScreen(onGoHome = { navController.replaceAll(HomeRoute) })
        }
        composable<HomeRoute> { HomeScreen() }
    }
}

/** The route the app starts at for a launch [SplashDestination]. */
fun SplashDestination.startRoute(): Any = when (this) {
    SplashDestination.Home -> HomeRoute
    SplashDestination.Welcome -> WelcomeRoute
    is SplashDestination.Setup -> step.route()
}

/** Opens [route] with an empty back stack. */
private fun NavHostController.replaceAll(route: Any) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
}

private fun SetupStep.route(): Any = when (this) {
    SetupStep.District -> DistrictRoute
    SetupStep.Interests -> InterestsRoute
    SetupStep.Notifications -> NotificationsRoute
    SetupStep.AllSet -> AllSetRoute
}
