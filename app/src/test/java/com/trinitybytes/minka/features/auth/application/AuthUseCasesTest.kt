package com.trinitybytes.minka.features.auth.application

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import com.trinitybytes.minka.features.auth.presentation.FakeSessionStore
import com.trinitybytes.minka.features.onboarding.FakeProgressStore
import com.trinitybytes.minka.features.onboarding.application.BeginOnboarding
import com.trinitybytes.minka.features.onboarding.application.LaunchTarget
import com.trinitybytes.minka.features.onboarding.application.ResolveLaunch
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class AuthUseCasesTest {

    private val session = Session("token-1", User("user-ana", "Ana Quispe", "ana.quispe@gmail.com"))
    private val newGoogleSession = Session("token-2", User("user-new", "Ana Q. · Trabajo", "ana@reflowtech.pe"), isNewAccount = true)

    private val repository = object : AuthRepository {
        var revoked: String? = null
        var revokeAnswer: AuthOutcome<Unit> = Outcome.Success(Unit)
        override suspend fun signOut(token: String): AuthOutcome<Unit> { revoked = token; return revokeAnswer }
        var answer: AuthOutcome<Session> = Outcome.Success(session)
        override suspend fun signIn(contact: String, password: String) = answer
        override suspend fun signInWithGoogle(accountId: String) = answer
        override suspend fun verifyCode(challengeId: String, code: String) = answer
        override suspend fun googleAccounts(): AuthOutcome<List<GoogleAccount>> = TODO()
        override suspend fun requestPasswordReset(contact: String): AuthOutcome<PasswordReset> = TODO()
        override suspend fun register(registration: Registration): AuthOutcome<VerificationChallenge> = TODO()
        override suspend fun resendCode(challengeId: String): AuthOutcome<VerificationChallenge> = TODO()
    }

    /** A device whose storage cannot be written, as with a full disk. */
    private val brokenStore = object : SessionStore {
        override val session = MutableStateFlow<Session?>(null)
        override suspend fun save(session: Session) = throw IOException("disk full")
        override suspend fun clear() = throw IOException("disk full")
    }

    @Test
    fun `new google account does not save a session when onboarding fails`() = runTest {
        repository.answer = Outcome.Success(newGoogleSession)
        val sessions = FakeSessionStore()
        val broken = object : OnboardingProgressStore {
            override val progress = MutableStateFlow<OnboardingProgress?>(null)
            override suspend fun save(progress: OnboardingProgress) {
                throw IOException()
            }
        }
        assertEquals(
            Outcome.Failure(AuthError.Unexpected),
            SignInWithGoogle(repository, sessions, BeginOnboarding(broken))("google"),
        )
        assertNull(sessions.session.value)
    }

    @Test
    fun `sign out revokes the session and clears the device even offline`() = runTest {
        val sessions = FakeSessionStore().apply { save(this@AuthUseCasesTest.session) }
        repository.revokeAnswer = Outcome.Failure(AuthError.Offline)
        SignOut(sessions, repository)()
        assertEquals(session.token, repository.revoked)
        assertNull(sessions.session.value)
    }

    @Test
    fun `a session that cannot be stored is an unexpected failure, not a crash`() = runTest {
        val progress = FakeProgressStore()

        assertEquals(Outcome.Failure(AuthError.Unexpected), SignIn(repository, brokenStore)("ana", "Minka2026!"))
        assertEquals(
            Outcome.Failure(AuthError.Unexpected),
            SignInWithGoogle(repository, brokenStore, BeginOnboarding(progress))("google-ana"),
        )
        assertEquals(Outcome.Failure(AuthError.Unexpected), VerifyRegistration(repository, brokenStore, BeginOnboarding(FakeProgressStore()))("c-1", "482915"))
        assertEquals(
            Outcome.Failure(AuthError.Unexpected),
            VerifyRegistration(repository, FakeSessionStore(), { throw IOException("disk full") })("c-1", "482915"),
        )
    }

    @Test
    fun `a new google account starts the profile setup before its session is stored`() = runTest {
        repository.answer = Outcome.Success(newGoogleSession)
        val sessions = FakeSessionStore()
        val progress = FakeProgressStore()

        assertEquals(Outcome.Success(newGoogleSession), SignInWithGoogle(repository, sessions, BeginOnboarding(progress))("google-ana-work"))

        assertEquals(OnboardingProgress.Pending("user-new", SetupStep.District), progress.progress.value)
        assertEquals(LaunchTarget.Setup(SetupStep.District), ResolveLaunch(sessions, progress)())
    }

    @Test
    fun `an existing google account goes straight home`() = runTest {
        val sessions = FakeSessionStore()
        val progress = FakeProgressStore()

        assertEquals(Outcome.Success(session), SignInWithGoogle(repository, sessions, BeginOnboarding(progress))("google-ana"))

        assertNull(progress.progress.value)
        assertEquals(session, sessions.session.value)
        assertEquals(LaunchTarget.Home, ResolveLaunch(sessions, progress)())
    }
}
