package com.trinitybytes.minka.features.auth.domain

import com.trinitybytes.minka.features.onboarding.domain.SetupStep

/** Starts setup before the new session becomes visible. */
fun interface NewAccountHook {
    suspend fun begin(userId: String): Boolean
}

/** Returns the pending setup step after a successful sign-in.
 * Auth intentionally reads onboarding's domain [SetupStep] as a navigation target.
 */
fun interface PendingSetupResolver {
    suspend fun pendingStep(): SetupStep?
}
