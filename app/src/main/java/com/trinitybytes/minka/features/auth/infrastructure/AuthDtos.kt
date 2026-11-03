package com.trinitybytes.minka.features.auth.infrastructure

import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import kotlinx.serialization.Serializable

/** Wire format of the auth endpoints, shared by the Ktor adapter and the mock server. */

@Serializable
data class SignInRequest(val contact: String, val password: String)

@Serializable
data class GoogleSignInRequest(val accountId: String)

@Serializable
data class PasswordResetRequest(val contact: String)

@Serializable
data class RegistrationRequest(
    val name: String,
    val contact: String,
    val password: String,
    val termsVersion: String,
    val termsAcceptedAt: String,
)

@Serializable
data class VerificationRequest(val code: String)

@Serializable
data class UserDto(val id: String, val name: String, val contact: String) {
    fun toDomain() = User(id, name, contact)
}

@Serializable
data class SessionDto(val token: String, val user: UserDto) {
    fun toDomain() = Session(token, user.toDomain())
}

@Serializable
data class GoogleAccountDto(val id: String, val name: String, val email: String) {
    fun toDomain() = GoogleAccount(id, name, email)
}

@Serializable
data class PasswordResetDto(val destination: String, val expiresInMinutes: Int, val resendAfterSeconds: Int) {
    fun toDomain() = PasswordReset(destination, expiresInMinutes, resendAfterSeconds)
}

@Serializable
data class ChallengeDto(val id: String, val destination: String, val attemptsLeft: Int, val resendAfterSeconds: Int) {
    fun toDomain() = VerificationChallenge(id, destination, attemptsLeft, resendAfterSeconds)
}
