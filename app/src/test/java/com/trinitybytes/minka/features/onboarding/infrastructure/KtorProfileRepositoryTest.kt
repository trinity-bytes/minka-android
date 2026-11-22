package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.infrastructure.MockAuthServer
import com.trinitybytes.minka.features.auth.infrastructure.KtorAuthRepository
import com.trinitybytes.minka.features.auth.infrastructure.DEMO_EMAIL
import com.trinitybytes.minka.features.auth.infrastructure.DEMO_PASSWORD
import com.trinitybytes.minka.features.auth.presentation.FakeSessionStore
import kotlinx.coroutines.runBlocking
import java.time.Clock
import com.trinitybytes.minka.core.network.InMemoryMockStorage
import com.trinitybytes.minka.core.network.MockApi
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.core.network.minkaHttpClient
import com.trinitybytes.minka.features.onboarding.domain.Coordinates
import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.Profile
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration

class KtorProfileRepositoryTest {

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
    private val auth = MockAuthServer(storage, Clock.systemUTC())
    private val client = minkaHttpClient(MockApi(auth.routes() + MockProfileServer(storage, auth).routes(), Duration.ZERO).engine())
    private val sessions = FakeSessionStore().apply {
        runBlocking { save((KtorAuthRepository(client).signIn(DEMO_EMAIL, DEMO_PASSWORD) as Outcome.Success).value) }
    }
    private fun repository() = KtorProfileRepository(client, sessions)
    private val repo = repository()

    private fun <T> Outcome<T, ProfileError>.value(): T = (this as Outcome.Success).value

    @Test
    fun `the catalogs list the Figma districts and interests first and in order`() = runTest {
        assertEquals(
            listOf("Miraflores", "San Isidro", "Barranco", "Santiago de Surco", "Jesús María"),
            repo.districts().value().take(5).map { it.name },
        )
        assertEquals(
            listOf("Hogar", "Ropa", "Libros", "Tecnología", "Deportes", "Niños", "Herramientas", "Plantas", "Servicios", "Arte", "Música", "Cocina"),
            repo.interests().value().map { it.name },
        )
    }

    @Test
    fun `the nearest district comes from the coarse location, or the default without one`() = runTest {
        assertEquals(District("barranco", "Barranco"), repo.nearestDistrict(Coordinates(-12.148, -77.020)).value())
        assertEquals(District("miraflores", "Miraflores"), repo.nearestDistrict(null).value())
    }

    @Test
    fun `the choices are saved on the profile and survive a new client`() = runTest {
        assertEquals(Outcome.Success(Unit), repo.saveDistrict("san-isidro"))
        assertEquals(Outcome.Success(Unit), repo.saveInterests(listOf("libros", "plantas")))

        assertEquals(
            Profile(District("san-isidro", "San Isidro"), listOf("libros", "plantas")),
            repository().profile().value(),
        )
    }

    @Test
    fun `profile GET is read only and sign out invalidates the token`() = runTest {
        repo.saveDistrict("san-isidro")
        val before = storage.memory.read("profiles")
        val authBefore = storage.memory.read("auth")
        assertEquals(District("san-isidro", "San Isidro"), repo.profile().value().district)
        assertEquals(before, storage.memory.read("profiles"))
        assertEquals(authBefore, storage.memory.read("auth"))
        val authRepository = KtorAuthRepository(client)
        assertEquals(Outcome.Success(Unit), authRepository.signOut(sessions.session.value!!.token))
        assertEquals(Outcome.Failure(ProfileError.Unexpected), repo.profile())
    }

    @Test
    fun `unknown choices are refused`() = runTest {
        assertEquals(Outcome.Failure(ProfileError.Unexpected), repo.saveDistrict("atlantida"))
        assertEquals(Outcome.Failure(ProfileError.Unexpected), repo.saveInterests(listOf("libros", "yates")))
        assertEquals(Profile(null, emptyList()), repo.profile().value())
    }

    @Test
    fun `a failed write leaves the profile as it was on disk`() = runTest {
        repo.saveDistrict("san-isidro")
        storage.failWrites = true

        assertEquals(Outcome.Failure(ProfileError.Offline), repo.saveDistrict("barranco"))

        storage.failWrites = false
        assertEquals(Profile(District("san-isidro", "San Isidro"), emptyList()), repo.profile().value())
    }
}
