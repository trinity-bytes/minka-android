package com.trinitybytes.minka.features.auth.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

val TEST_NOW: Instant = Instant.parse("2026-11-03T12:00:00Z")
val TEST_CLOCK: Clock = Clock.fixed(TEST_NOW, ZoneOffset.UTC)

val TEST_CHALLENGE = VerificationChallenge("challenge-1", "luis.mamani@gmail.com", attemptsLeft = 5, resendAfterSeconds = 60)
val TEST_SESSION = Session("token-1", User("user-luis", "Luis Mamani", "luis.mamani@gmail.com"))

/** Registration half of the auth port, scripted per test. */
class FakeRegistrationRepository : AuthRepository {
    val registrations = mutableListOf<Registration>()
    val verifications = mutableListOf<Pair<String, String>>()
    val resends = mutableListOf<String>()

    var registerAnswer: suspend () -> AuthOutcome<VerificationChallenge> = { Outcome.Success(TEST_CHALLENGE) }
    var verifyAnswer: suspend (String) -> AuthOutcome<Session> = { Outcome.Success(TEST_SESSION) }
    var resendAnswer: suspend () -> AuthOutcome<VerificationChallenge> = { Outcome.Success(TEST_CHALLENGE) }

    override suspend fun register(registration: Registration): AuthOutcome<VerificationChallenge> {
        registrations += registration
        return registerAnswer()
    }

    override suspend fun verifyCode(challengeId: String, code: String): AuthOutcome<Session> {
        verifications += challengeId to code
        return verifyAnswer(code)
    }

    override suspend fun resendCode(challengeId: String): AuthOutcome<VerificationChallenge> {
        resends += challengeId
        return resendAnswer()
    }

    override suspend fun signIn(contact: String, password: String): AuthOutcome<Session> = TODO()
    override suspend fun googleAccounts(): AuthOutcome<List<GoogleAccount>> = TODO()
    override suspend fun signInWithGoogle(accountId: String): AuthOutcome<Session> = TODO()
    override suspend fun requestPasswordReset(contact: String): AuthOutcome<PasswordReset> = TODO()
}

class FakeSessionStore : SessionStore {
    override val session = MutableStateFlow<Session?>(null)
    override suspend fun save(session: Session) {
        this.session.value = session
    }
    override suspend fun clear() {
        session.value = null
    }
}
