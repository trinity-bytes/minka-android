package com.trinitybytes.minka.features.auth.domain

import com.trinitybytes.minka.core.domain.Outcome
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Version of the terms and privacy policy the user accepts when registering (Figma 1.12). */
const val TERMS_VERSION = "1.0"

data class User(
    val id: String,
    val name: String,
    /** Email or phone the account was created with. */
    val contact: String,
)

data class Session(val token: String, val user: User)

/** An account offered by the Google picker (Figma 1.09). */
data class GoogleAccount(val id: String, val name: String, val email: String)

data class PasswordReset(
    val destination: String,
    val expiresInMinutes: Int,
    val resendAfterSeconds: Int,
)

data class Registration(
    val name: String,
    val contact: String,
    val password: String,
    val termsVersion: String,
    val termsAcceptedAt: Instant,
)

/** A pending 6-digit code sent to [destination] to confirm a registration (Figma 1.14). */
data class VerificationChallenge(
    val id: String,
    val destination: String,
    val attemptsLeft: Int,
    val resendAfterSeconds: Int,
)

sealed interface AuthError {
    data class InvalidCredentials(val attemptsLeft: Int) : AuthError
    data class AccountLocked(val retryAfterSeconds: Long) : AuthError
    data object ContactTaken : AuthError
    data class WrongCode(val attemptsLeft: Int) : AuthError
    /** The code ran out of attempts; a new one has to be requested. */
    data object CodeExpired : AuthError
    data class TooSoon(val retryAfterSeconds: Long) : AuthError
    data object Offline : AuthError
    data object Unexpected : AuthError
}

typealias AuthOutcome<T> = Outcome<T, AuthError>

/** Port to the identity service. */
interface AuthRepository {
    suspend fun signIn(contact: String, password: String): AuthOutcome<Session>
    suspend fun googleAccounts(): AuthOutcome<List<GoogleAccount>>
    suspend fun signInWithGoogle(accountId: String): AuthOutcome<Session>
    suspend fun requestPasswordReset(contact: String): AuthOutcome<PasswordReset>
    suspend fun register(registration: Registration): AuthOutcome<VerificationChallenge>
    suspend fun verifyCode(challengeId: String, code: String): AuthOutcome<Session>
    suspend fun resendCode(challengeId: String): AuthOutcome<VerificationChallenge>
}

/** Port to the device storage that keeps the signed-in session across launches. */
interface SessionStore {
    /** The current session, or null when signed out. */
    val session: Flow<Session?>
    suspend fun save(session: Session)
    suspend fun clear()
}
