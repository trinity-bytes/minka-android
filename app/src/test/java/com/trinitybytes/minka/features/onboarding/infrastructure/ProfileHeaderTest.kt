package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.core.network.minkaHttpClient
import com.trinitybytes.minka.features.auth.presentation.FakeSessionStore
import com.trinitybytes.minka.features.onboarding.ANA
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileHeaderTest {
    @Test
    fun `every profile operation carries the stored bearer token`() = runTest {
        val seen = mutableListOf<String?>()
        val engine = MockEngine { request ->
            seen += request.headers[HttpHeaders.Authorization]
            val body = when {
                request.url.encodedPath.endsWith("nearest") -> "{\"id\":\"miraflores\",\"name\":\"Miraflores\"}"
                request.url.encodedPath.endsWith("districts") || request.url.encodedPath.endsWith("interests") && request.method.value == "GET" -> "[]"
                else -> "{\"district\":null,\"interestIds\":[]}"
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val store = FakeSessionStore().apply { session.value = ANA }
        val repo = KtorProfileRepository(minkaHttpClient(engine), store)
        repo.districts(); repo.nearestDistrict(null); repo.interests()
        repo.saveDistrict("miraflores"); repo.saveInterests(emptyList()); repo.profile()
        assertEquals(List(6) { "Bearer ${ANA.token}" }, seen)
    }
}
