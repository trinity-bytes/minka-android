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
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration as KotlinDuration
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAuthServerLockAndStorageTest {

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
        var writes = 0
        override suspend fun read(key: String) = memory.read(key)
        override suspend fun write(key: String, value: String) {
            if (failWrites) throw IOException("disk full")
            memory.write(key, value)
            writes++
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
    fun `stored credentials are hashed and still authenticate`() = runTest {
        assertEquals("Ana Quispe", repo.signIn(DEMO_EMAIL, DEMO_PASSWORD).value().user.name)
        assertFalse(storage.memory.read("auth")!!.contains(DEMO_PASSWORD))
        assertTrue(repo.signIn(DEMO_EMAIL, "wrong").error() is AuthError.InvalidCredentials)
    }

    @Test
    fun `session lookup migrates a legacy document once`() = runTest {
        val legacy = """{"accounts":[{"id":"user-ana","name":"Ana","email":"$DEMO_EMAIL",""" +
            """"phone":null,"password":"$DEMO_PASSWORD"}],"sessions":{"t":"user-ana"}}"""
        storage.memory.write("auth", legacy)
        val server = MockAuthServer(storage, clock)

        assertEquals("user-ana", server.userFor("t"))
        val migrated = storage.memory.read("auth")!!
        assertFalse(migrated.contains(DEMO_PASSWORD))
        assertEquals(1, storage.writes)
        assertEquals("user-ana", server.userFor("t"))
        assertEquals(migrated, storage.memory.read("auth"))
        assertEquals(1, storage.writes)
        assertEquals("Ana", repository().signIn(DEMO_EMAIL, DEMO_PASSWORD).value().user.name)
    }

    @Test
    fun `failed migration leaves legacy state intact and can be retried`() = runTest {
        val legacy = """{"accounts":[{"id":"u","name":"Ana","email":"$DEMO_EMAIL",""" +
            """"phone":null,"password":"$DEMO_PASSWORD"}],"sessions":{"t":"u"}}"""
        storage.memory.write("auth", legacy)
        val server = MockAuthServer(storage, clock)
        storage.failWrites = true

        try {
            server.userFor("t")
            org.junit.Assert.fail("Migration must report a failed write")
        } catch (_: IOException) {
            assertEquals(legacy, storage.memory.read("auth"))
        }
        storage.failWrites = false
        assertEquals("u", server.userFor("t"))
        assertFalse(storage.memory.read("auth")!!.contains(DEMO_PASSWORD))
    }

    @Test
    fun `lock on email then phone with right password is locked`() = runTest {
        repeat(5) { repo.signIn(DEMO_EMAIL, "nope") }
        assertEquals(
            AuthError.AccountLocked(retryAfterSeconds = 900),
            repo.signIn("+51 987 654 321", DEMO_PASSWORD).error(),
        )
    }

    @Test
    fun `unknown contact locks per contact and does not touch the demo account`() = runTest {
        repeat(4) { assertTrue(repo.signIn("ghost@x.pe", "a").error() is AuthError.InvalidCredentials) }
        assertEquals(AuthError.AccountLocked(retryAfterSeconds = 900), repo.signIn("ghost@x.pe", "a").error())
        assertEquals(AuthError.AccountLocked(retryAfterSeconds = 900), repo.signIn("GHOST@x.pe", "a").error())
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4), repo.signIn("other@x.pe", "a").error())
        assertEquals("Ana Quispe", repo.signIn(DEMO_EMAIL, DEMO_PASSWORD).value().user.name)
    }

    @Test
    fun `lock expiry clears the account counter for every contact`() = runTest {
        repeat(5) { repo.signIn(DEMO_EMAIL, "nope") }
        now = now.plus(Duration.ofMinutes(14))
        assertTrue(repo.signIn(DEMO_PHONE, DEMO_PASSWORD).error() is AuthError.AccountLocked)
        now = now.plus(Duration.ofMinutes(1))
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4), repo.signIn(DEMO_PHONE, "nope").error())
        assertEquals("Ana Quispe", repo.signIn(DEMO_EMAIL, DEMO_PASSWORD).value().user.name)
        // success reset the counter
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4), repo.signIn(DEMO_EMAIL, "nope").error())
    }

    @Test
    fun `failures under the limit persist across restart and survive a known-account key`() = runTest {
        repeat(3) { repo.signIn(DEMO_EMAIL, "nope") }
        val restarted = repository()
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 1), restarted.signIn(DEMO_PHONE, "nope").error())
    }

    @Test
    fun `state written in the pre-change format still decodes`() = runTest {
        val legacy = """{"accounts":[{"id":"user-ana","name":"Ana Quispe","email":"$DEMO_EMAIL",""" +
            """"phone":"$DEMO_PHONE","password":"$DEMO_PASSWORD"},{"id":"u2","name":"Luis",""" +
            """"email":"luis@x.pe","password":"Pw123456!"}],"failedSignIns":{"$DEMO_EMAIL":2},""" +
            """"lockedUntil":{},"challenges":{"c1":{"id":"c1","registration":{"name":"Rosa",""" +
            """"contact":"rosa@x.pe","password":"Pw123456!","termsVersion":"$TERMS_VERSION",""" +
            """"termsAcceptedAt":"2026-11-03T11:59:00Z"},"attemptsLeft":5,"resendAvailableAt":0}},""" +
            """"sessions":{"t":"user-ana"}}"""
        storage.memory.write("auth", legacy)
        val r = repository()
        assertEquals("Luis", r.signIn("luis@x.pe", "Pw123456!").value().user.name)
        assertFalse(storage.memory.read("auth")!!.contains("Pw123456!"))
        assertFalse(storage.memory.read("auth")!!.contains(DEMO_PASSWORD))
        assertEquals("Rosa", r.verifyCode("c1", DEMO_VERIFICATION_CODE).value().user.name)
        // legacy contact-keyed counter is ignored by the new account key (advisory)
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4), r.signIn(DEMO_EMAIL, "nope").error())
    }

    @Test
    fun `resend and registration still work and verify after resend`() = runTest {
        val c = repo.register(registration).value()
        now = now.plusSeconds(60)
        assertEquals(5, repo.resendCode(c.id).value().attemptsLeft)
        assertEquals("Luis Mamani", repo.verifyCode(c.id, DEMO_VERIFICATION_CODE).value().user.name)
        assertEquals(AuthError.ContactTaken, repo.register(registration).error())
    }

    @Test
    fun `failed write on sign-in failure does not count the attempt`() = runTest {
        storage.failWrites = true
        assertEquals(AuthError.Offline, repo.signIn(DEMO_EMAIL, "nope").error())
        storage.failWrites = false
        assertEquals(AuthError.InvalidCredentials(attemptsLeft = 4), repo.signIn(DEMO_EMAIL, "nope").error())
    }

    @Test
    fun `verified challenge after 5 wrong codes needs a resend`() = runTest {
        val c = repo.register(registration).value()
        repo.verifyCode(c.id, DEMO_VERIFICATION_CODE).value()
        repeat(5) { repo.verifyCode(c.id, "000000") }
        assertEquals(AuthError.CodeExpired, repo.verifyCode(c.id, DEMO_VERIFICATION_CODE).error())
    }
}
