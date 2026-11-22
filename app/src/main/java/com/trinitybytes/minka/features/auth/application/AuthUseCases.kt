package com.trinitybytes.minka.features.auth.application

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.core.domain.onSuccess
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.TERMS_VERSION
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import com.trinitybytes.minka.features.onboarding.application.BeginOnboarding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/** Signs in with contact and password; on success the session is kept on the device. */
class SignIn @Inject constructor(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
) {
    suspend operator fun invoke(contact: String, password: String): AuthOutcome<Session> = storingOnDevice {
        repository.signIn(contact, password).onSuccess { sessionStore.save(it) }
    }
}

class GetGoogleAccounts @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): AuthOutcome<List<GoogleAccount>> = repository.googleAccounts()
}

/**
 * Signs in with a Google account. An account created by this sign-in starts the profile setup, saved
 * before the session as registration does, so the next launch resumes the setup instead of Home.
 */
class SignInWithGoogle @Inject constructor(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
    private val beginOnboarding: BeginOnboarding,
) {
    suspend operator fun invoke(accountId: String): AuthOutcome<Session> = storingOnDevice {
        repository.signInWithGoogle(accountId).onSuccess {
            if (it.isNewAccount) beginOnboarding(it.user.id)
            sessionStore.save(it)
        }
    }
}

class RequestPasswordReset @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(contact: String): AuthOutcome<PasswordReset> =
        repository.requestPasswordReset(contact)
}

class Register @Inject constructor(
    private val repository: AuthRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(registration: Registration): AuthOutcome<VerificationChallenge> =
        repository.register(registration)

    /** Registers with the current terms (1.12): their version and the moment they were accepted are sent. */
    suspend operator fun invoke(name: String, contact: String, password: String): AuthOutcome<VerificationChallenge> =
        invoke(Registration(name, contact, password, TERMS_VERSION, clock.instant()))
}

/** Confirms a registration with its code; the new account is signed in right away. */
class VerifyRegistration @Inject constructor(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
) {
    /**
     * [beforeSignIn] runs once the code is accepted and before the session is stored, so whatever it
     * saves already exists whenever the session does, even if the app dies in between.
     */
    suspend operator fun invoke(
        challengeId: String,
        code: String,
        beforeSignIn: suspend (Session) -> Unit = {},
    ): AuthOutcome<Session> = storingOnDevice {
        repository.verifyCode(challengeId, code).onSuccess {
            beforeSignIn(it)
            sessionStore.save(it)
        }
    }
}

class ResendCode @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(challengeId: String): AuthOutcome<VerificationChallenge> =
        repository.resendCode(challengeId)
}

/** The current session, restored from the device on launch (Figma 1.01). */
class ObserveSession @Inject constructor(private val sessionStore: SessionStore) {
    operator fun invoke(): Flow<Session?> = sessionStore.session
}

class SignOut @Inject constructor(private val sessionStore: SessionStore, private val repository: AuthRepository) {
    suspend operator fun invoke() {
        sessionStore.session.first()?.let { repository.signOut(it.token) }
        sessionStore.clear()
    }
}

/** A device storage failure while keeping the result is reported as [AuthError.Unexpected] instead of crashing. */
private inline fun <T> storingOnDevice(block: () -> AuthOutcome<T>): AuthOutcome<T> = try {
    block()
} catch (_: IOException) {
    Outcome.Failure(AuthError.Unexpected)
}
