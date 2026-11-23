package com.trinitybytes.minka.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.trinitybytes.minka.features.auth.presentation.PasswordRecoveryScreen
import com.trinitybytes.minka.features.auth.presentation.RegisterScreen
import com.trinitybytes.minka.features.auth.presentation.SignInScreen
import com.trinitybytes.minka.features.auth.presentation.VerifyCodeScreen
import com.trinitybytes.minka.features.auth.presentation.VerifyCodeViewModel
import com.trinitybytes.minka.features.items.presentation.publish.PublishScreen
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import com.trinitybytes.minka.features.onboarding.presentation.AllSetScreen
import com.trinitybytes.minka.features.onboarding.presentation.DistrictScreen
import com.trinitybytes.minka.features.onboarding.presentation.InterestsScreen
import com.trinitybytes.minka.features.onboarding.presentation.NotificationsScreen
import com.trinitybytes.minka.features.onboarding.presentation.SplashDestination
import com.trinitybytes.minka.features.onboarding.presentation.SplashUiState
import com.trinitybytes.minka.features.onboarding.presentation.WelcomeScreen
import kotlinx.serialization.Serializable

/** Invisible first route, under the system splash until launch is resolved (Figma 1.01). */
@Serializable
data object SplashRoute : NavKey

@Serializable
data object WelcomeRoute : NavKey

/** Sign-in (Figma 1.05–1.09); [withGoogle] opens the Google account picker right away. */
@Serializable
data class SignInRoute(val withGoogle: Boolean = false) : NavKey

/** Password recovery (Figma 1.10–1.11). */
@Serializable
data object PasswordRecoveryRoute : NavKey

/** Registration data (Figma 1.12–1.13). */
@Serializable
data object RegisterRoute : NavKey

/**
 * Code verification (Figma 1.14–1.15) for [challengeId]. Its fields are passed to the
 * ViewModel and populate the keys it reads from SavedStateHandle.
 */
@Serializable
data class VerifyCodeRoute(
    val challengeId: String,
    val destination: String,
    val attemptsLeft: Int,
    val resendAfterSeconds: Int,
) : NavKey

/** Profile setup, district (Figma 1.16–1.18). */
@Serializable
data object DistrictRoute : NavKey

/** Profile setup, interests (Figma 1.19). */
@Serializable
data object InterestsRoute : NavKey

/** Profile setup, notifications (Figma 1.20). */
@Serializable
data object NotificationsRoute : NavKey

/** Profile setup, "Todo listo" (Figma 1.21). */
@Serializable
data object AllSetRoute : NavKey

@Serializable
data object HomeRoute : NavKey

/** Publicar (Figma 5.01–5.11), full screen; with [draftId] it resumes that draft (audit P1). */
@Serializable
data class PublishRoute(val draftId: String? = null) : NavKey

/**
 * Composed from the first frame so the Nav3 back stack restores after process death. [SplashRoute]
 * stays invisible beneath the system splash, resolves [launch], then replaces itself with the
 * resolved destination. User navigation taps use `dropUnlessResumed` to drop repeated actions
 * during a transition, avoiding duplicate pushes or double back navigation.
 */
@Composable
fun MinkaNavHost(launch: SplashUiState) {
    val stack = rememberNavBackStack(SplashRoute)
    fun replace(route: NavKey) {
        stack.clear()
        stack.add(route)
    }
    NavDisplay(
        backStack = stack,
        onBack = { if (stack.size > 1) stack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SplashRoute> {
                LaunchedEffect(launch) {
                    if (launch is SplashUiState.Ready && stack.lastOrNull() == SplashRoute) {
                        replace(launch.destination.startRoute())
                    }
                }
            }
            entry<WelcomeRoute> {
                WelcomeScreen(
                    onCreateAccount = dropUnlessResumed { stack.add(RegisterRoute) },
                    onSignIn = dropUnlessResumed { stack.add(SignInRoute()) },
                    onContinueWithGoogle = dropUnlessResumed { stack.add(SignInRoute(true)) },
                )
            }
            entry<SignInRoute> { key ->
                SignInScreen(
                    withGoogle = key.withGoogle,
                    onBack = dropUnlessResumed { if (stack.size > 1) stack.removeLastOrNull() },
                    onSignedIn = { replace(it?.route() ?: HomeRoute) },
                    onRecoverPassword = dropUnlessResumed { stack.add(PasswordRecoveryRoute) },
                    onRegister = dropUnlessResumed { stack.add(RegisterRoute) },
                )
            }
            entry<PasswordRecoveryRoute> {
                PasswordRecoveryScreen(
                    onBack = dropUnlessResumed { if (stack.size > 1) stack.removeLastOrNull() },
                )
            }
            entry<RegisterRoute> {
                RegisterScreen(
                    onBack = dropUnlessResumed { if (stack.size > 1) stack.removeLastOrNull() },
                    onCodeSent = { challenge ->
                        stack.add(
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
            entry<VerifyCodeRoute> { key ->
                val viewModel = hiltViewModel<VerifyCodeViewModel, VerifyCodeViewModel.Factory>(
                    creationCallback = { it.create(key) },
                )
                VerifyCodeScreen(
                    onBack = dropUnlessResumed { if (stack.size > 1) stack.removeLastOrNull() },
                    onVerified = { replace(DistrictRoute) },
                    onContactSupport = {},
                    viewModel = viewModel,
                )
            }
            entry<DistrictRoute> {
                DistrictScreen(
                    onBack = if (stack.size > 1) {
                        dropUnlessResumed { stack.removeLastOrNull() }
                    } else {
                        null
                    },
                    onChosen = { stack.add(InterestsRoute) },
                )
            }
            entry<InterestsRoute> {
                InterestsScreen(
                    onBack = dropUnlessResumed {
                        if (stack.size > 1) {
                            stack.removeLastOrNull()
                        } else {
                            replace(DistrictRoute)
                        }
                    },
                    onDone = { replace(NotificationsRoute) },
                )
            }
            entry<NotificationsRoute> {
                NotificationsScreen(onDone = { replace(AllSetRoute) })
            }
            entry<AllSetRoute> {
                AllSetScreen(onGoHome = { replace(HomeRoute) })
            }
            // These callbacks run outside any entry, where dropUnlessResumed sees the activity
            // lifecycle; the stack guards drop the second tap instead.
            shellEntries(
                onPublish = { if (stack.lastOrNull() !is PublishRoute) pushOnce(stack, PublishRoute()) },
                onResumeDraft = { draftId ->
                    if (stack.lastOrNull() !is PublishRoute) pushOnce(stack, PublishRoute(draftId))
                },
                onClose = { popIf(stack) { it is PublishRoute } },
            )
        },
    )
}

private fun EntryProviderScope<NavKey>.shellEntries(
    onPublish: () -> Unit,
    onResumeDraft: (String) -> Unit,
    onClose: () -> Unit,
) {
    entry<HomeRoute> { MinkaShell(onPublish = onPublish, onResumeDraft = onResumeDraft) }
    publishEntries(onClose)
}

private fun EntryProviderScope<NavKey>.publishEntries(onClose: () -> Unit) {
    // There is no listing detail yet, so "Ver publicación" leaves the flow like the close icon does.
    entry<PublishRoute> { key ->
        PublishScreen(route = key, onClose = onClose, onViewListing = { onClose() })
    }
}

/** The route the app starts at for a launch [SplashDestination]. */
fun SplashDestination.startRoute(): NavKey = when (this) {
    SplashDestination.Home -> HomeRoute
    SplashDestination.Welcome -> WelcomeRoute
    is SplashDestination.Setup -> step.route()
}

private fun SetupStep.route(): NavKey = when (this) {
    SetupStep.District -> DistrictRoute
    SetupStep.Interests -> InterestsRoute
    SetupStep.Notifications -> NotificationsRoute
    SetupStep.AllSet -> AllSetRoute
}
