package com.trinitybytes.minka.features.onboarding.infrastructure

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trinitybytes.minka.features.auth.infrastructure.DataStoreSessionStore
import com.trinitybytes.minka.features.onboarding.ANA
import com.trinitybytes.minka.features.onboarding.application.LaunchTarget
import com.trinitybytes.minka.features.onboarding.application.ResolveLaunch
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingProgressStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** A DataStore on [name] that lives in its own scope, so a second one can "relaunch" on the same file. */
    private fun TestScope.dataStore(name: String, scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("$name.preferences_pb") }

    private fun TestScope.launchScope() = CoroutineScope(Job() + StandardTestDispatcher(testScheduler))

    @Test
    fun `the progress persists across launches with its own keys`() = runTest {
        val first = launchScope()
        val store = DataStoreOnboardingProgressStore(dataStore("onboarding", first))
        assertNull(store.progress.first())

        store.save(OnboardingProgress.Pending(ANA.user.id, SetupStep.Interests))
        first.cancel()

        val second = launchScope()
        val reopened = dataStore("onboarding", second)
        assertEquals(
            OnboardingProgress.Pending(ANA.user.id, SetupStep.Interests),
            DataStoreOnboardingProgressStore(reopened).progress.first(),
        )
        val prefs = reopened.data.first()
        assertEquals(ANA.user.id, prefs[stringPreferencesKey("onboarding_user_id")])
        assertEquals("Interests", prefs[stringPreferencesKey("onboarding_next_step")])

        DataStoreOnboardingProgressStore(reopened).save(OnboardingProgress.Completed(ANA.user.id))
        assertEquals(OnboardingProgress.Completed(ANA.user.id), DataStoreOnboardingProgressStore(reopened).progress.first())
        assertEquals("completed", reopened.data.first()[stringPreferencesKey("onboarding_next_step")])
        second.cancel()
    }

    @Test
    fun `after a restart the splash resumes the saved step and the session is untouched`() = runTest {
        val first = launchScope()
        val sessions = DataStoreSessionStore(dataStore("session", first))
        sessions.save(ANA)
        DataStoreOnboardingProgressStore(dataStore("onboarding", first))
            .save(OnboardingProgress.Pending(ANA.user.id, SetupStep.Notifications))
        first.cancel()

        val second = launchScope()
        val restoredSessions = DataStoreSessionStore(dataStore("session", second))
        val restoredProgress = DataStoreOnboardingProgressStore(dataStore("onboarding", second))

        assertEquals(LaunchTarget.Setup(SetupStep.Notifications), ResolveLaunch(restoredSessions, restoredProgress)())
        assertEquals(ANA, restoredSessions.session.first())

        restoredProgress.save(OnboardingProgress.Completed(ANA.user.id))
        assertEquals(LaunchTarget.Home, ResolveLaunch(restoredSessions, restoredProgress)())

        restoredSessions.clear()
        assertEquals(LaunchTarget.Welcome, ResolveLaunch(restoredSessions, restoredProgress)())
        second.cancel()
    }
}
