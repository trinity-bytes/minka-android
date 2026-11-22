package com.trinitybytes.minka.features.onboarding.application

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.onboarding.FakeProfileRepository
import com.trinitybytes.minka.features.onboarding.FakeProgressStore
import com.trinitybytes.minka.features.onboarding.signedInSessionStore
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class OnboardingUseCasesTest {

    /** Device storage that can be neither read nor written. */
    private val brokenProgress = object : OnboardingProgressStore {
        override val progress: Flow<OnboardingProgress?> = flow { throw IOException("unreadable") }
        override suspend fun save(progress: OnboardingProgress) = throw IOException("disk full")
    }
    private val brokenSessions = object : SessionStore {
        override val session: Flow<Session?> = flow { throw IOException("unreadable") }
        override suspend fun save(session: Session) = throw IOException("disk full")
        override suspend fun clear() = throw IOException("disk full")
    }

    @Test
    fun `a setup step that cannot be stored is an unexpected failure, not a crash`() = runTest {
        val repository = FakeProfileRepository()

        assertEquals(
            Outcome.Failure(ProfileError.Unexpected),
            ChooseDistrict(signedInSessionStore(), repository, brokenProgress)("miraflores"),
        )
        assertEquals(
            Outcome.Failure(ProfileError.Unexpected),
            SaveInterests(signedInSessionStore(), repository, brokenProgress)(listOf("libros")),
        )
        assertEquals(
            Outcome.Failure(ProfileError.Unexpected),
            ChooseDistrict(brokenSessions, repository, FakeProgressStore())("miraflores"),
        )
    }

    @Test
    fun `a saved step still moves the setup on`() = runTest {
        val progress = FakeProgressStore()

        assertEquals(Outcome.Success(Unit), ChooseDistrict(signedInSessionStore(), FakeProfileRepository(), progress)("miraflores"))
        assertEquals(OnboardingProgress.Pending("user-ana", SetupStep.Interests), progress.progress.value)
    }

    @Test
    fun `an unreadable device opens on welcome instead of crashing`() = runTest {
        assertEquals(LaunchTarget.Welcome, ResolveLaunch(brokenSessions, FakeProgressStore())())
        assertEquals(LaunchTarget.Welcome, ResolveLaunch(signedInSessionStore(), brokenProgress)())
    }
}
