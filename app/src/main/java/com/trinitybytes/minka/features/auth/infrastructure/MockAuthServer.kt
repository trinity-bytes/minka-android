package com.trinitybytes.minka.features.auth.infrastructure

import com.trinitybytes.minka.core.network.MockDocument
import com.trinitybytes.minka.core.network.MockReply
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockSessions
import com.trinitybytes.minka.core.network.MockUserNames
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.features.auth.domain.normalizeContact
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.serialization.Serializable

/** Seed account of the mock API (README, "Cuenta demo"). */
const val DEMO_EMAIL = "ana.quispe@gmail.com"
const val DEMO_PHONE = "987654321"
const val DEMO_PASSWORD = "Minka2026!"

/** The mock API never sends email or SMS: every verification code is this one (README). */
const val DEMO_VERIFICATION_CODE = "482915"

/**
 * The identity half of the in-app mock API. It applies the rules of Figma flow 01: 5 failed
 * sign-ins lock the account (all its contacts) for 15 minutes, a code allows 5 attempts and can be resent after 60
 * seconds, and password reset links expire in 30 minutes.
 */
class MockAuthServer(
    storage: MockStorage,
    private val clock: Clock,
) : MockSessions, MockUserNames {
    private val document = MockDocument(storage, STORAGE_KEY, AuthDb.serializer(), AuthDb::migrate) {
        AuthDb.seed()
    }

    fun routes(): Set<MockRoute> = setOf(
        MockRoute(HttpMethod.Post, "auth/sessions") { signIn(it.body()) },
        MockRoute(HttpMethod.Delete, "auth/sessions/current") { call ->
            val token = call.bearerToken()
                ?: return@MockRoute MockReply.error(HttpStatusCode.Unauthorized, "unauthorized")
            transaction { db ->
                if (db.sessions.remove(token) == null) MockReply.error(HttpStatusCode.Unauthorized, "unauthorized")
                else MockReply.json(HttpStatusCode.OK, true)
            }
        },
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
        val account = db.accounts.firstOrNull { it.matches(contact) }
        // One counter and one lock per account, whichever of its contacts is typed; unknown contacts count apart.
        val key = account?.let { "$ACCOUNT_KEY_PREFIX${it.id}" } ?: contact
        db.lockedUntil[key]?.let { until ->
            if (until > now) return@transaction locked(until - now)
            db.lockedUntil.remove(key)
            db.failedSignIns.remove(key)
        }
        if (account == null || !PasswordHash.matches(request.password, account.password)) {
            val failures = (db.failedSignIns[key] ?: 0) + 1
            if (failures >= MAX_SIGN_IN_ATTEMPTS) {
                db.failedSignIns.remove(key)
                db.lockedUntil[key] = now + LOCK_MILLIS
                return@transaction locked(LOCK_MILLIS)
            }
            db.failedSignIns[key] = failures
            return@transaction MockReply.error(
                HttpStatusCode.Unauthorized,
                AuthErrorCodes.INVALID_CREDENTIALS,
                attemptsLeft = MAX_SIGN_IN_ATTEMPTS - failures,
            )
        }
        db.failedSignIns.remove(key)
        openSession(db, account)
    }

    private suspend fun signInWithGoogle(request: GoogleSignInRequest): MockReply = transaction { db ->
        val google = GOOGLE_ACCOUNTS.firstOrNull { it.id == request.accountId }
            ?: return@transaction MockReply.error(HttpStatusCode.NotFound, "not_found")
        db.accounts.firstOrNull { it.matches(google.email) }?.let { return@transaction openSession(db, it) }
        // A new account still has the profile setup ahead (1.16-1.21): the client is told so.
        val account = AccountRecord(newId(), google.name, google.email, phone = null, password = null)
        db.accounts += account
        openSession(db, account, HttpStatusCode.Created, isNewAccount = true)
    }

    private suspend fun register(request: RegistrationRequest): MockReply = transaction { db ->
        val contact = normalizeContact(request.contact)
        if (db.accounts.any { it.matches(contact) }) {
            return@transaction MockReply.error(HttpStatusCode.Conflict, AuthErrorCodes.CONTACT_TAKEN)
        }
        // Only the newest code of a contact can be confirmed.
        db.challenges.values.removeAll { it.accountId == null && it.registration.contact == contact }
        val challenge = ChallengeRecord(
            id = newId(),
            registration = request.copy(contact = contact, password = PasswordHash.hash(request.password)),
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
        // Already verified: the right code opens a new session, e.g. when the client lost the first one.
        challenge.accountId?.let { accountId ->
            val account = db.accounts.firstOrNull { it.id == accountId }
                ?: return@transaction MockReply.error(HttpStatusCode.NotFound, "not_found")
            return@transaction openSession(db, account, HttpStatusCode.Created)
        }
        val registration = challenge.registration
        if (db.accounts.any { it.matches(registration.contact) }) {
            db.challenges.remove(id)
            return@transaction MockReply.error(HttpStatusCode.Conflict, AuthErrorCodes.CONTACT_TAKEN)
        }
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
        db.challenges[id] = challenge.copy(accountId = account.id)
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

    private fun openSession(
        db: AuthDb,
        account: AccountRecord,
        status: HttpStatusCode = HttpStatusCode.OK,
        isNewAccount: Boolean = false,
    ): MockReply {
        val token = UUID.randomUUID().toString()
        db.sessions[token] = account.id
        val user = UserDto(account.id, account.name, account.contact)
        return MockReply.json(status, SessionDto(token, user, isNewAccount))
    }

    private fun locked(millis: Long) = MockReply.error(
        HttpStatusCode.Locked,
        AuthErrorCodes.ACCOUNT_LOCKED,
        retryAfterSeconds = seconds(millis),
    )

    private fun seconds(millis: Long) = (millis + 999) / 1000

    private fun newId() = UUID.randomUUID().toString()

    override suspend fun userFor(token: String): String? = document.read { it.sessions[token] }

    override suspend fun nameOf(userId: String): String? =
        document.read { db -> db.accounts.firstOrNull { it.id == userId }?.name }

    /** Runs [block] on a copy of the stored state, one request at a time. */
    private suspend fun transaction(block: (AuthDb) -> MockReply): MockReply = document.update(block)

    private fun ChallengeRecord.toDto() = ChallengeDto(
        id = id,
        destination = registration.contact,
        attemptsLeft = attemptsLeft,
        resendAfterSeconds = RESEND_AFTER_SECONDS,
    )

    private companion object {
        const val STORAGE_KEY = "auth"

        /** Prefix of the failure and lock keys of a known account, so no typed contact can collide with them. */
        const val ACCOUNT_KEY_PREFIX = "account:"
        const val MAX_SIGN_IN_ATTEMPTS = 5
        const val LOCK_MILLIS = 15 * 60 * 1000L
        const val MAX_CODE_ATTEMPTS = 5
        const val RESEND_AFTER_SECONDS = 60
        const val RESET_LINK_MINUTES = 30

        /** Accounts the mock Google picker shows (Figma 1.09). */
        val GOOGLE_ACCOUNTS = listOf(
            GoogleAccountDto("google-ana", "Ana Quispe", DEMO_EMAIL),
            GoogleAccountDto("google-ana-work", "Ana Q. · Trabajo", "ana@reflowtech.pe"),
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
    /** The account the code created once verified; the challenge stays so it can be verified again. */
    val accountId: String? = null,
)

@Serializable
private data class AuthDb(
    val accounts: MutableList<AccountRecord> = mutableListOf(),
    val failedSignIns: MutableMap<String, Int> = mutableMapOf(),
    val lockedUntil: MutableMap<String, Long> = mutableMapOf(),
    val challenges: MutableMap<String, ChallengeRecord> = mutableMapOf(),
    val sessions: MutableMap<String, String> = mutableMapOf(),
) {
    fun migrate(): Boolean {
        var changed = false
        accounts.replaceAll { account ->
            val password = account.password?.let(PasswordHash::migrate)
            if (password != account.password) changed = true
            account.copy(password = password)
        }
        challenges.replaceAll { _, challenge ->
            val password = PasswordHash.migrate(challenge.registration.password)
            if (password != challenge.registration.password) changed = true
            challenge.copy(registration = challenge.registration.copy(password = password))
        }
        return changed
    }

    companion object {
        fun seed() = AuthDb(
            accounts = mutableListOf(
                AccountRecord(
                    id = "user-ana",
                    name = "Ana Quispe",
                    email = DEMO_EMAIL,
                    phone = DEMO_PHONE,
                    password = PasswordHash.hash(DEMO_PASSWORD),
                ),
            ),
        )
    }
}

private object PasswordHash {
    private const val PREFIX = "pbkdf2-sha256$"
    private const val ITERATIONS = 120_000

    fun migrate(value: String): String = if (value.startsWith(PREFIX)) value else hash(value)

    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return PREFIX + Base64.getEncoder().encodeToString(salt) + "$" +
            Base64.getEncoder().encodeToString(derive(password, salt))
    }

    fun matches(password: String, stored: String?): Boolean {
        if (stored == null || !stored.startsWith(PREFIX)) return false
        return try {
            val parts = stored.removePrefix(PREFIX).split('$')
            val salt = Base64.getDecoder().decode(parts[0])
            val expected = Base64.getDecoder().decode(parts[1])
            MessageDigest.isEqual(expected, derive(password, salt))
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
