package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
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
    private fun repository() = KtorProfileRepository(
        minkaHttpClient(MockApi(MockProfileServer(storage).routes(), Duration.ZERO).engine()),
    )
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
        assertEquals(Outcome.Success(Unit), repo.saveDistrict("user-ana", "san-isidro"))
        assertEquals(Outcome.Success(Unit), repo.saveInterests("user-ana", listOf("libros", "plantas")))

        assertEquals(
            Profile(District("san-isidro", "San Isidro"), listOf("libros", "plantas")),
            repository().profile("user-ana").value(),
        )
        assertEquals(Profile(null, emptyList()), repo.profile("user-luis").value())
    }

    @Test
    fun `unknown choices are refused`() = runTest {
        assertEquals(Outcome.Failure(ProfileError.Unexpected), repo.saveDistrict("user-ana", "atlantida"))
        assertEquals(Outcome.Failure(ProfileError.Unexpected), repo.saveInterests("user-ana", listOf("libros", "yates")))
        assertEquals(Profile(null, emptyList()), repo.profile("user-ana").value())
    }

    @Test
    fun `a failed write leaves the profile as it was on disk`() = runTest {
        repo.saveDistrict("user-ana", "san-isidro")
        storage.failWrites = true

        assertEquals(Outcome.Failure(ProfileError.Offline), repo.saveDistrict("user-ana", "barranco"))

        storage.failWrites = false
        assertEquals(Profile(District("san-isidro", "San Isidro"), emptyList()), repo.profile("user-ana").value())
    }
}
