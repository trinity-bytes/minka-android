package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.infrastructure.KtorAuthRepository
import com.trinitybytes.minka.features.auth.infrastructure.DEMO_EMAIL
import com.trinitybytes.minka.features.auth.infrastructure.DEMO_PASSWORD
import com.trinitybytes.minka.features.auth.infrastructure.MockAuthServer
import io.ktor.client.request.put
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.Clock
import com.trinitybytes.minka.core.network.InMemoryMockStorage
import com.trinitybytes.minka.core.network.MockApi
import com.trinitybytes.minka.core.network.minkaHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration

class ProfileAuthorizationTest {
    @Test
    fun `profile catalogs reject missing and unknown sessions`() = runTest {
        val storage = InMemoryMockStorage()
        val client = minkaHttpClient(MockApi(MockProfileServer(storage, MockAuthServer(storage, Clock.systemUTC())).routes(), Duration.ZERO).engine())
        assertEquals(HttpStatusCode.Unauthorized, client.get("profile/districts").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("profile/interests") {
            header(HttpHeaders.Authorization, "Bearer invalid")
        }.status)
        for (token in listOf(null, "invalid")) {
            fun io.ktor.client.request.HttpRequestBuilder.authenticate() {
                if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(HttpStatusCode.Unauthorized, client.get("profile/districts") { authenticate() }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.post("profile/districts/nearest") { authenticate() }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("profile/interests") { authenticate() }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("me/profile") { authenticate() }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.put("me/profile/district") { authenticate() }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.put("me/profile/interests") { authenticate() }.status)
        }
    }
    @Test
    fun `profile routes resolve users from sessions and reads do not write`() = runTest {
        val storage = InMemoryMockStorage()
        val auth = MockAuthServer(storage, Clock.systemUTC())
        val client = minkaHttpClient(MockApi(auth.routes() + MockProfileServer(storage, auth).routes(), Duration.ZERO).engine())
        val repo = KtorAuthRepository(client)
        val ana = (repo.signIn(DEMO_EMAIL, DEMO_PASSWORD) as Outcome.Success).value.token
        val other = (repo.signInWithGoogle("google-ana-work") as Outcome.Success).value.token
        suspend fun put(token: String) = client.put("me/profile/district") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(DistrictChoiceRequest("barranco"))
        }
        assertEquals(HttpStatusCode.Unauthorized, put("invalid").status)
        assertEquals(HttpStatusCode.OK, put(ana).status)
        val before = storage.read("profiles")
        val own = client.get("me/profile") { header(HttpHeaders.Authorization, "Bearer $ana") }.body<ProfileDto>()
        val another = client.get("me/profile") { header(HttpHeaders.Authorization, "Bearer $other") }.body<ProfileDto>()
        assertEquals("barranco", own.district?.id)
        assertEquals(null, another.district)
        assertEquals(before, storage.read("profiles"))
        assertEquals(HttpStatusCode.Unauthorized, client.post("profile/districts/nearest") {
            header(HttpHeaders.Authorization, "Bearer invalid")
        }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.put("me/profile/interests") {}.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("me/profile").status)
    }

}
