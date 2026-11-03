package com.trinitybytes.minka.features.auth.infrastructure

import com.trinitybytes.minka.core.network.MinkaJson
import com.trinitybytes.minka.core.network.MockCall
import com.trinitybytes.minka.core.network.MockReply
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.features.auth.domain.normalizeContact
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.time.Clock
import java.util.UUID

/** Seed account of the mock API (README, "Cuenta demo"). */
const val DEMO_EMAIL = "ana.quispe@gmail.com"
const val DEMO_PHONE = "987654321"
const val DEMO_PASSWORD = "Minka2026!"

/** The mock API never sends email or SMS: every verification code is this one (README). */
const val DEMO_VERIFICATION_CODE = "482915"

/**
 * The identity half of the in-app mock API. It applies the rules of Figma flow 01: 5 failed
 * sign-ins lock the contact for 15 minutes, a code allows 5 attempts and can be resent after 60
 * seconds, and password reset links expire in 30 minutes.
 */
class MockAuthServer(
    private val storage: MockStorage,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private var cache: AuthDb? = null

    fun routes(): Set<MockRoute> = setOf(
        MockRoute(HttpMethod.Post, "auth/sessions") { signIn(it.body()) },
        MockRoute(HttpMethod.Get, "auth/google/accounts") {
            MockReply.json(HttpStatusCode.OK, GOOGLE_ACCOUNTS)
        },
        MockRoute(HttpMethod.Post, "auth/google/sessions") { signInWithGoogle(it.body()) },
        MockRoute(HttpMethod.Post, "auth/password-resets") { call ->
            val request = call.body<PasswordResetRequest>()
            // Same answer whether or not the contact exists, so accounts cannot be discovered.
            MockReply.json(
                HttpStatusCode.Accepted,
                PasswordResetDto(normalizeContact(request.contact), RESET_LINK_MINUTES, RESEND_AFTER_SECONDS),
            )
        },
        MockRoute(HttpMethod.Post, "auth/registrations") { register(it.body()) },
        MockRoute(HttpMethod.Post, "auth/registrations/{id}/verification") { call ->
            verify(call.pathParam("id"), call.body())
        },
        MockRoute(HttpMethod.Post, "auth/registrations/{id}/code") { call -> resend(call.pathParam("id")) },
    )

    private suspend fun signIn(request: SignInRequest): MockReply = transaction { db ->
        val contact = normalizeContact(request.contact)
        val now = clock.millis()
        db.lockedUntil[contact]?.let { until ->
            if (until > now) return@transaction locked(until - now)
            db.lockedUntil.remove(contact)
            db.failedSignIns.remove(contact)
        }
        val account = db.accounts.firstOrNull { it.matches(contact) }
        if (account == null || account.password != request.password) {
            val failures = (db.failedSignIns[contact] ?: 0) + 1
            if (failures >= MAX_SIGN_IN_ATTEMPTS) {
                db.failedSignIns.remove(contact)
                db.lockedUntil[contact] = now + LOCK_MILLIS
                return@transaction locked(LOCK_MILLIS)
            }
            db.failedSignIns[contact] = failures
            return@transaction MockReply.error(
                HttpStatusCode.Unauthorized,
                AuthErrorCodes.INVALID_CREDENTIALS,
                attemptsLeft = MAX_SIGN_IN_ATTEMPTS - failures,
            )
        }
        db.failedSignIns.remove(contact)
        openSession(db, account)
    }

    private suspend fun signInWithGoogle(request: GoogleSignInRequest): MockReply = transaction { db ->
        val google = GOOGLE_ACCOUNTS.firstOrNull { it.id == request.accountId }
            ?: return@transaction MockReply.error(HttpStatusCode.NotFound, "not_found")
        val account = db.accounts.firstOrNull { it.matches(google.email) }
            ?: AccountRecord(newId(), google.name, google.email, phone = null, password = null).also(db.accounts::add)
        openSession(db, account)
    }

    private suspend fun register(request: RegistrationRequest): MockReply = transaction { db ->
        val contact = normalizeContact(request.contact)
        if (db.accounts.any { it.matches(contact) }) {
            return@transaction MockReply.error(HttpStatusCode.Conflict, AuthErrorCodes.CONTACT_TAKEN)
        }
        val challenge = ChallengeRecord(
            id = newId(),
            registration = request.copy(contact = contact),
            attemptsLeft = MAX_CODE_ATTEMPTS,
            resendAvailableAt = clock.millis() + RESEND_AFTER_SECONDS * 1000L,
        )
        db.challenges[challenge.id] = challenge
        MockReply.json(HttpStatusCode.Created, challenge.toDto())
    }

    private suspend fun verify(id: String, request: VerificationRequest): MockReply = transaction { db ->
        val challenge = db.challenges[id]
            ?: return@transaction MockReply.error(HttpStatusCode.NotFound, "not_found")
        if (challenge.attemptsLeft == 0) {
            return@transaction MockReply.error(HttpStatusCode.Gone, AuthErrorCodes.CODE_EXPIRED)
        }
        if (request.code != DEMO_VERIFICATION_CODE) {
            val left = challenge.attemptsLeft - 1
            db.challenges[id] = challenge.copy(attemptsLeft = left)
            return@transaction if (left == 0) {
                MockReply.error(HttpStatusCode.Gone, AuthErrorCodes.CODE_EXPIRED)
            } else {
                MockReply.error(HttpStatusCode.UnprocessableEntity, AuthErrorCodes.WRONG_CODE, attemptsLeft = left)
            }
        }
        db.challenges.remove(id)
        val registration = challenge.registration
        val isPhone = '@' !in registration.contact
        val account = AccountRecord(
            id = newId(),
            name = registration.name.trim(),
            email = registration.contact.takeUnless { isPhone },
            phone = registration.contact.takeIf { isPhone },
            password = registration.password,
            termsVersion = registration.termsVersion,
            termsAcceptedAt = registration.termsAcceptedAt,
        )
        db.accounts += account
        openSession(db, account, HttpStatusCode.Created)
    }

    private suspend fun resend(id: String): MockReply = transaction { db ->
        val challenge = db.challenges[id]
            ?: return@transaction MockReply.error(HttpStatusCode.NotFound, "not_found")
        val wait = challenge.resendAvailableAt - clock.millis()
        if (wait > 0) {
            return@transaction MockReply.error(
                HttpStatusCode.TooManyRequests,
                AuthErrorCodes.TOO_SOON,
                retryAfterSeconds = seconds(wait),
            )
        }
        val renewed = challenge.copy(
            attemptsLeft = MAX_CODE_ATTEMPTS,
            resendAvailableAt = clock.millis() + RESEND_AFTER_SECONDS * 1000L,
        )
        db.challenges[id] = renewed
        MockReply.json(HttpStatusCode.OK, renewed.toDto())
    }

    private fun openSession(db: AuthDb, account: AccountRecord, status: HttpStatusCode = HttpStatusCode.OK): MockReply {
        val token = UUID.randomUUID().toString()
        db.sessions[token] = account.id
        return MockReply.json(status, SessionDto(token, UserDto(account.id, account.name, account.contact)))
    }

    private fun locked(millis: Long) = MockReply.error(
        HttpStatusCode.Locked,
        AuthErrorCodes.ACCOUNT_LOCKED,
        retryAfterSeconds = seconds(millis),
    )

    private fun seconds(millis: Long) = (millis + 999) / 1000

    private fun newId() = UUID.randomUUID().toString()

    /** Runs [block] on the loaded state and persists it afterwards, one request at a time. */
    private suspend fun transaction(block: (AuthDb) -> MockReply): MockReply = mutex.withLock {
        val db = cache ?: (storage.read(STORAGE_KEY)?.let { MinkaJson.decodeFromString<AuthDb>(it) } ?: AuthDb.seed())
            .also { cache = it }
        block(db).also { storage.write(STORAGE_KEY, MinkaJson.encodeToString(db)) }
    }

    private fun ChallengeRecord.toDto() = ChallengeDto(
        id = id,
        destination = registration.contact,
        attemptsLeft = attemptsLeft,
        resendAfterSeconds = RESEND_AFTER_SECONDS,
    )

    private companion object {
        const val STORAGE_KEY = "auth"
        const val MAX_SIGN_IN_ATTEMPTS = 5
        const val LOCK_MILLIS = 15 * 60 * 1000L
        const val MAX_CODE_ATTEMPTS = 5
        const val RESEND_AFTER_SECONDS = 60
        const val RESET_LINK_MINUTES = 30

        /** Accounts the mock Google picker shows (Figma 1.09). */
        val GOOGLE_ACCOUNTS = listOf(
            GoogleAccountDto("google-ana", "Ana Quispe", DEMO_EMAIL),
            GoogleAccountDto("google-ana-work", "Ana Quispe", "ana@trinitybytes.pe"),
        )
    }
}

@Serializable
private data class AccountRecord(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    /** Null for accounts created with Google. */
    val password: String?,
    val termsVersion: String? = null,
    val termsAcceptedAt: String? = null,
) {
    val contact: String get() = email ?: phone.orEmpty()

    fun matches(normalizedContact: String) = normalizedContact == email || normalizedContact == phone
}

@Serializable
private data class ChallengeRecord(
    val id: String,
    val registration: RegistrationRequest,
    val attemptsLeft: Int,
    val resendAvailableAt: Long,
)

@Serializable
private data class AuthDb(
    val accounts: MutableList<AccountRecord> = mutableListOf(),
    val failedSignIns: MutableMap<String, Int> = mutableMapOf(),
    val lockedUntil: MutableMap<String, Long> = mutableMapOf(),
    val challenges: MutableMap<String, ChallengeRecord> = mutableMapOf(),
    val sessions: MutableMap<String, String> = mutableMapOf(),
) {
    companion object {
        fun seed() = AuthDb(
            accounts = mutableListOf(
                AccountRecord(
                    id = "user-ana",
                    name = "Ana Quispe",
                    email = DEMO_EMAIL,
                    phone = DEMO_PHONE,
                    password = DEMO_PASSWORD,
                ),
            ),
        )
    }
}
