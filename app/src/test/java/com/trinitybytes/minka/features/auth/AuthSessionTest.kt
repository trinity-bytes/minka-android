package com.trinitybytes.minka.features.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.application.SignIn
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.GoogleAccount
import com.trinitybytes.minka.features.auth.domain.PasswordReset
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.auth.domain.VerificationChallenge
import com.trinitybytes.minka.features.auth.infrastructure.DataStoreSessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthSessionTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val session = Session("token-1", User("user-ana", "Ana Quispe", "ana.quispe@gmail.com"))

    private fun TestScope.store(name: String = "session") = DataStoreSessionStore(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.root.resolve("$name.preferences_pb") },
    )

    @Test
    fun `the session store keeps the session until it is cleared`() = runTest {
        val store = store()
        assertNull(store.session.first())

        store.save(session)
        assertEquals(session, store.session.first())

        store.clear()
        assertNull(store.session.first())
    }

    @Test
    fun `signing in saves the session only when it succeeds`() = runTest {
        val store = store()
        val repository = FakeRepository()

        repository.signInResult = Outcome.Failure(AuthError.InvalidCredentials(4))
        assertEquals(Outcome.Failure(AuthError.InvalidCredentials(4)), SignIn(repository, store)("ana", "x"))
        assertNull(store.session.first())

        repository.signInResult = Outcome.Success(session)
        SignIn(repository, store)("ana", "Minka2026!")
        assertEquals(session, store.session.first())
    }

    private class FakeRepository : AuthRepository {
        var signInResult: AuthOutcome<Session> = Outcome.Failure(AuthError.Unexpected)
        override suspend fun signIn(contact: String, password: String) = signInResult
        override suspend fun googleAccounts(): AuthOutcome<List<GoogleAccount>> = TODO()
        override suspend fun signInWithGoogle(accountId: String): AuthOutcome<Session> = TODO()
        override suspend fun requestPasswordReset(contact: String): AuthOutcome<PasswordReset> = TODO()
        override suspend fun register(registration: Registration): AuthOutcome<VerificationChallenge> = TODO()
        override suspend fun verifyCode(challengeId: String, code: String): AuthOutcome<Session> = TODO()
        override suspend fun resendCode(challengeId: String): AuthOutcome<VerificationChallenge> = TODO()
    }
}
