package com.trinitybytes.minka.features.auth.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.core.network.ApiError
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class KtorAuthRepository @Inject constructor(private val client: HttpClient) : AuthRepository {

    override suspend fun signIn(contact: String, password: String): AuthOutcome<Session> =
        call<SessionDto, Session>(SessionDto::toDomain) { post("auth/sessions", SignInRequest(contact, password)) }

    override suspend fun googleAccounts(): AuthOutcome<List<GoogleAccount>> =
        call<List<GoogleAccountDto>, List<GoogleAccount>>({ list -> list.map { it.toDomain() } }) {
            client.get("auth/google/accounts")
        }

    override suspend fun signInWithGoogle(accountId: String): AuthOutcome<Session> =
        call<SessionDto, Session>(SessionDto::toDomain) { post("auth/google/sessions", GoogleSignInRequest(accountId)) }

    override suspend fun requestPasswordReset(contact: String): AuthOutcome<PasswordReset> =
        call<PasswordResetDto, PasswordReset>(PasswordResetDto::toDomain) {
            post("auth/password-resets", PasswordResetRequest(contact))
        }

    override suspend fun register(registration: Registration): AuthOutcome<VerificationChallenge> =
        call<ChallengeDto, VerificationChallenge>(ChallengeDto::toDomain) {
            post(
                "auth/registrations",
                RegistrationRequest(
                    name = registration.name,
                    contact = registration.contact,
                    password = registration.password,
                    termsVersion = registration.termsVersion,
                    termsAcceptedAt = registration.termsAcceptedAt.toString(),
                ),
            )
        }

    override suspend fun verifyCode(challengeId: String, code: String): AuthOutcome<Session> =
        call<SessionDto, Session>(SessionDto::toDomain) {
            post("auth/registrations/$challengeId/verification", VerificationRequest(code))
        }

    override suspend fun resendCode(challengeId: String): AuthOutcome<VerificationChallenge> =
        call<ChallengeDto, VerificationChallenge>(ChallengeDto::toDomain) {
            client.post("auth/registrations/$challengeId/code")
        }

    private suspend inline fun <reified B> post(path: String, body: B): HttpResponse = client.post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend inline fun <reified D, T> call(
        toDomain: (D) -> T,
        request: () -> HttpResponse,
    ): AuthOutcome<T> = try {
        val response = request()
        if (response.status.isSuccess()) {
            Outcome.Success(toDomain(response.body<D>()))
        } else {
            Outcome.Failure(response.body<ApiError>().toAuthError())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        Outcome.Failure(AuthError.Offline)
    } catch (_: Exception) {
        Outcome.Failure(AuthError.Unexpected)
    }

    private fun ApiError.toAuthError(): AuthError = when (code) {
        AuthErrorCodes.INVALID_CREDENTIALS -> AuthError.InvalidCredentials(attemptsLeft ?: 0)
        AuthErrorCodes.ACCOUNT_LOCKED -> AuthError.AccountLocked(retryAfterSeconds ?: 0)
        AuthErrorCodes.CONTACT_TAKEN -> AuthError.ContactTaken
        AuthErrorCodes.WRONG_CODE -> AuthError.WrongCode(attemptsLeft ?: 0)
        AuthErrorCodes.CODE_EXPIRED -> AuthError.CodeExpired
        AuthErrorCodes.TOO_SOON -> AuthError.TooSoon(retryAfterSeconds ?: 0)
        else -> AuthError.Unexpected
    }
}

/** Stable `code` values of [ApiError] for the auth endpoints. */
object AuthErrorCodes {
    const val INVALID_CREDENTIALS = "invalid_credentials"
    const val ACCOUNT_LOCKED = "account_locked"
    const val CONTACT_TAKEN = "contact_taken"
    const val WRONG_CODE = "wrong_code"
    const val CODE_EXPIRED = "code_expired"
    const val TOO_SOON = "too_soon"
}
