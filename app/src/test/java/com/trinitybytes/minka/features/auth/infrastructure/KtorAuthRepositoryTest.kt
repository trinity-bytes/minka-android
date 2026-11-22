package com.trinitybytes.minka.features.auth.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.core.network.InMemoryMockStorage
import com.trinitybytes.minka.core.network.MinkaJson
import com.trinitybytes.minka.core.network.MockApi
import com.trinitybytes.minka.core.network.MockConnection
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.core.network.minkaHttpClient
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.TERMS_VERSION
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.io.IOException
import java.time.ZoneOffset
import kotlin.time.Duration as KotlinDuration

class KtorAuthRepositoryTest {

    private var now = Instant.parse("2026-11-03T12:00:00Z")
    private val clock = object : Clock() {
        override fun instant(): Instant = now
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
    }
    /** Persists like [InMemoryMockStorage] until [failWrites] is set, then every write fails as a full disk would. */
    private val storage = object : MockStorage {
        val memory = InMemoryMockStorage()
        var failWrites = false
        override suspend fun read(key: String) = memory.read(key)
        override suspend fun write(key: String, value: String) {
            if (failWrites) throw IOException("disk full")
            memory.write(key, value)
        }
    }
    private fun repository() = KtorAuthRepository(
        minkaHttpClient(MockApi(MockAuthServer(storage, clock).routes(), KotlinDuration.ZERO).engine()),
    )
    private val repo = repository()

    private fun <T> Outcome<T, AuthError>.value(): T = (this as Outcome.Success).value
    private fun <T> Outcome<T, AuthError>.error(): AuthError = (this as Outcome.Failure).error

    private val registration = Registration(
        name = "Luis Mamani",
        contact = "Luis.Mamani@Gmail.com",
        password = "Trueque2026!",
        termsVersion = TERMS_VERSION,
        termsAcceptedAt = Instant.parse("2026-11-03T11:59:00Z"),
    )

    @Test
    fun `the demo account signs in with its email or phone`() = runTest {
        val byEmail = repo.signIn(DEMO_EMAIL, DEMO_PASSWORD).value()
        val byPhone = repo.signIn("+51 987 654 321", DEMO_PASSWORD).value()

        assertEquals("Ana Quispe", byEmail.user.name)
        assertEquals(byEmail.user.id, byPhone.user.id)
        assertTrue(byEmail.token.isNotBlank())
    }

    @Test
    fun `a wrong password reports the attempts left and the fifth one locks for 15 minutes`() = runTest {
        repeat(4) { attempt ->
            assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4 - attempt), repo.signIn(DEMO_EMAIL, "nope").error())
        }

        assertEquals(AuthError.AccountLocked(retryAfterSeconds = 900), repo.signIn(DEMO_EMAIL, "nope").error())
        assertEquals(AuthError.AccountLocked(retryAfterSeconds = 900), repo.signIn(DEMO_EMAIL, DEMO_PASSWORD).error())

        now += Duration.ofMinutes(15)
        assertEquals("Ana Quispe", repo.signIn(DEMO_EMAIL, DEMO_PASSWORD).value().user.name)
    }

    @Test
    fun `an unknown contact gets the same answer as a wrong password`() = runTest {
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4), repo.signIn("nadie@minka.pe", "x").error())
    }

    @Test
    fun `a password reset is accepted for any contact and expires in 30 minutes`() = runTest {
        val known = repo.requestPasswordReset(DEMO_EMAIL).value()
        val unknown = repo.requestPasswordReset("nadie@minka.pe").value()

        assertEquals(DEMO_EMAIL, known.destination)
        assertEquals(30, known.expiresInMinutes)
        assertEquals("nadie@minka.pe", unknown.destination)
    }

    @Test
    fun `a password reset by phone answers the normalized number and can be requested again after 60 seconds`() = runTest {
        val first = repo.requestPasswordReset("+51 987 654 321").value()
        val again = repo.requestPasswordReset("+51 987 654 321").value()

        assertEquals(PasswordReset("987654321", expiresInMinutes = 30, resendAfterSeconds = 60), first)
        assertEquals(first, again)
    }

    @Test
    fun `registering asks for a 6 digit code with 5 attempts and the right code opens a session`() = runTest {
        val challenge = repo.register(registration).value()
        assertEquals("luis.mamani@gmail.com", challenge.destination)
        assertEquals(5, challenge.attemptsLeft)

        val session: Session = repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).value()

        assertEquals("Luis Mamani", session.user.name)
        assertEquals("Luis Mamani", repo.signIn("luis.mamani@gmail.com", "Trueque2026!").value().user.name)
    }

    @Test
    fun `the accepted terms version and date are kept with the new account`() = runTest {
        val challenge = repo.register(registration).value()
        repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).value()

        val saved = MinkaJson.parseToJsonElement(checkNotNull(storage.read("auth"))).jsonObject
        val account = saved.getValue("accounts").jsonArray
            .map { it.jsonObject }
            .single { it["email"]?.jsonPrimitive?.content == "luis.mamani@gmail.com" }

        assertEquals(TERMS_VERSION, account.getValue("termsVersion").jsonPrimitive.content)
        assertEquals("2026-11-03T11:59:00Z", account.getValue("termsAcceptedAt").jsonPrimitive.content)
    }

    @Test
    fun `a taken contact cannot register again`() = runTest {
        assertEquals(AuthError.ContactTaken, repo.register(registration.copy(contact = DEMO_EMAIL)).error())
    }

    @Test
    fun `wrong codes count down and after 5 a new code is needed, which resets the attempts`() = runTest {
        val challenge = repo.register(registration).value()

        repeat(4) { attempt ->
            assertEquals(AuthError.WrongCode(attemptsLeft = 4 - attempt), repo.verifyCode(challenge.id, "000000").error())
        }
        assertEquals(AuthError.CodeExpired, repo.verifyCode(challenge.id, "000000").error())
        assertEquals(AuthError.CodeExpired, repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).error())

        now += Duration.ofSeconds(60)
        val renewed = repo.resendCode(challenge.id).value()
        assertEquals(5, renewed.attemptsLeft)
        assertEquals("Luis Mamani", repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).value().user.name)
    }

    @Test
    fun `a new code cannot be requested before the resend timer ends`() = runTest {
        val challenge = repo.register(registration).value()

        assertEquals(AuthError.TooSoon(retryAfterSeconds = 60), repo.resendCode(challenge.id).error())
    }

    @Test
    fun `email and phone of one account share one failure counter and one lock`() = runTest {
        repeat(2) { repo.signIn(DEMO_EMAIL, "nope") }
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 2), repo.signIn("+51 987 654 321", "nope").error())
        repo.signIn(DEMO_EMAIL, "nope")

        assertEquals(AuthError.AccountLocked(retryAfterSeconds = 900), repo.signIn(DEMO_PHONE, "nope").error())
        assertEquals(AuthError.AccountLocked(retryAfterSeconds = 900), repo.signIn(DEMO_EMAIL, DEMO_PASSWORD).error())
        assertEquals(AuthError.AccountLocked(retryAfterSeconds = 900), repo.signIn(DEMO_PHONE, DEMO_PASSWORD).error())
    }

    @Test
    fun `a contact taken while its registration was pending cannot be verified`() = runTest {
        val challenge = repo.register(registration.copy(contact = "ana@reflowtech.pe")).value()
        repo.signInWithGoogle("google-ana-work").value()

        assertEquals(AuthError.ContactTaken, repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).error())
    }

    @Test
    fun `registering again drops the older pending code for the same contact`() = runTest {
        val older = repo.register(registration).value()
        val newer = repo.register(registration).value()

        assertEquals(AuthError.Unexpected, repo.verifyCode(older.id, DEMO_VERIFICATION_CODE).error())
        assertEquals("Luis Mamani", repo.verifyCode(newer.id, DEMO_VERIFICATION_CODE).value().user.name)
    }

    @Test
    fun `a verified code opens a new session for its account when it is verified again`() = runTest {
        val challenge = repo.register(registration).value()
        val first = repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).value()

        assertEquals(AuthError.WrongCode(attemptsLeft = 4), repo.verifyCode(challenge.id, "000000").error())
        val again = repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).value()

        assertEquals(first.user, again.user)
        assertNotEquals(first.token, again.token)
    }

    @Test
    fun `a failed write leaves the server as it was on disk`() = runTest {
        val challenge = repo.register(registration).value()
        storage.failWrites = true

        assertEquals(AuthError.Offline, repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).error())

        storage.failWrites = false
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4), repo.signIn("luis.mamani@gmail.com", "Trueque2026!").error())
        assertEquals("Luis Mamani", repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).value().user.name)
    }

    @Test
    fun `google flags the session of an account it just created, and only that first time`() = runTest {
        assertTrue(repo.signInWithGoogle("google-ana-work").value().isNewAccount)
        assertFalse(repo.signInWithGoogle("google-ana-work").value().isNewAccount)
        assertFalse(repo.signInWithGoogle("google-ana").value().isNewAccount)
    }

    @Test
    fun `google access lists the device accounts and signs in with the chosen one`() = runTest {
        val accounts = repo.googleAccounts().value()
        val session = repo.signInWithGoogle(accounts.first().id).value()

        assertEquals(accounts.first().name, session.user.name)
        assertEquals(accounts.first().email, session.user.contact)
        assertFalse(session.isNewAccount)
    }

    @Test
    fun `the google picker offers the accounts drawn in figma 1_09`() = runTest {
        val accounts = repo.googleAccounts().value()

        assertEquals(
            listOf("Ana Quispe" to DEMO_EMAIL, "Ana Q. · Trabajo" to "ana@reflowtech.pe"),
            accounts.map { it.name to it.email },
        )
        assertEquals("Ana Q. · Trabajo", repo.signInWithGoogle(accounts[1].id).value().user.name)
    }

    @Test
    fun `the mock server state survives a restart`() = runTest {
        val challenge = repo.register(registration).value()
        repo.verifyCode(challenge.id, DEMO_VERIFICATION_CODE).value()

        val restarted = repository()

        assertEquals("Luis Mamani", restarted.signIn("luis.mamani@gmail.com", "Trueque2026!").value().user.name)
    }

    @Test
    fun `with the no-connection switch on the mock answers the offline error`() = runTest {
        var offline = true
        val repo = KtorAuthRepository(
            minkaHttpClient(MockApi(MockAuthServer(storage, clock).routes(), KotlinDuration.ZERO, MockConnection { offline }).engine()),
        )

        assertEquals(AuthError.Offline, repo.signIn("ana.quispe@gmail.com", "Minka2026!").error())
        assertEquals(AuthError.Offline, repo.requestPasswordReset("ana.quispe@gmail.com").error())

        offline = false
        assertEquals("ana.quispe@gmail.com", repo.signIn("ana.quispe@gmail.com", "Minka2026!").value().user.contact)
    }
}
