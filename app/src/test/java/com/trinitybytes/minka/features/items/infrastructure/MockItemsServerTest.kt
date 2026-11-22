package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.core.network.InMemoryMockStorage
import com.trinitybytes.minka.core.network.MockCall
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockSessions
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.core.network.MockUserNames
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.onboarding.infrastructure.ProfileDto
import io.ktor.http.HttpMethod
import java.time.Clock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MockItemsServerTest {
    private fun routeOf(storage: MockStorage, method: HttpMethod, vararg segments: String): MockRoute =
        MockItemsServer(
            storage,
            MockSessions { it.takeIf { token -> token != "revoked" } },
            Clock.systemUTC(),
            MockUserNames { it },
        ) { ProfileDto() }.routes().first { it.method == method && it.match(segments.toList()) != null }

    @Test
    fun `drafts and status changes persist when the server restarts`() = runTest {
        val storage = InMemoryMockStorage()
        val first = ItemsHarness(storage = storage)
        val listing = first.publishedDraft()
        first.repository.setStatus(listing.id, ListingStatus.PAUSED)
        val draft = first.repository.saveDraft(null, ItemsHarness.completeDraft("Otro")).value()
        val restarted = ItemsHarness(storage = storage)
        val paused = restarted.repository.myListings(ListingStatus.PAUSED).value().value
        assertEquals(listOf(listing.id), paused.map { it.id })
        assertEquals(listOf(draft.id), restarted.repository.myDrafts().value().map { it.id })
    }

    @Test
    fun `reading recent searches does not write server storage`() = runTest {
        val storage = object : MockStorage {
            var writes = 0

            override suspend fun read(key: String): String? = null

            override suspend fun write(key: String, value: String) {
                writes++
            }
        }
        val reply = routeOf(storage, HttpMethod.Get, "me", "recent-searches")
            .handler(MockCall(emptyMap(), "", "Bearer ana"))
        assertEquals(200, reply.status.value)
        assertEquals(0, storage.writes)
    }

    @Test
    fun `a request without a known token is unauthorized`() = runTest {
        val route = routeOf(InMemoryMockStorage(), HttpMethod.Get, "feed")
        assertEquals(401, route.handler(MockCall(emptyMap(), "", null)).status.value)
        assertEquals(401, route.handler(MockCall(emptyMap(), "", "Bearer revoked")).status.value)
    }

    @Test
    fun `the owner name of a listing comes from the account, not from the request`() = runTest {
        val harness = ItemsHarness(accountNames = mapOf("ana" to "Ana Torres"))
        val listing = harness.publishedDraft()
        assertEquals("Ana Torres", listing.owner.name)
    }
}
