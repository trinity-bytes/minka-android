package com.trinitybytes.minka.core.datastore

import com.trinitybytes.minka.features.auth.infrastructure.DataStoreSessionStore
import com.trinitybytes.minka.features.onboarding.application.LaunchTarget
import com.trinitybytes.minka.features.onboarding.application.ResolveLaunch
import com.trinitybytes.minka.features.onboarding.infrastructure.DataStoreOnboardingProgressStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** A corrupt preferences file must not crash every launch: it is replaced with empty preferences. */
class MinkaDataStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.launchScope() = CoroutineScope(Job() + StandardTestDispatcher(testScheduler))

    private fun corruptFile(name: String) =
        folder.root.resolve("$name.preferences_pb").apply { writeBytes(byteArrayOf(0x0a, 0x7f, 0x01, 0x02)) }

    @Test
    fun `corrupt session and progress files route to welcome instead of crashing`() = runTest {
        val scope = launchScope()
        val session = corruptFile("session")
        val onboarding = corruptFile("onboarding")

        val launch = ResolveLaunch(
            DataStoreSessionStore(minkaPreferencesDataStore(scope) { session }),
            DataStoreOnboardingProgressStore(minkaPreferencesDataStore(scope) { onboarding }),
        )

        assertEquals(LaunchTarget.Welcome, launch())
        scope.cancel()
    }
}
