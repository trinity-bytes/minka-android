package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.core.network.InMemoryMockStorage
import com.trinitybytes.minka.core.network.MockApi
import com.trinitybytes.minka.core.network.MockConnection
import com.trinitybytes.minka.core.network.MockSessions
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.core.network.MockUserNames
import com.trinitybytes.minka.core.network.minkaHttpClient
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.ItemsOutcome
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.onboarding.infrastructure.KtorProfileRepository
import com.trinitybytes.minka.features.onboarding.infrastructure.MockProfileServer
import java.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ListingCache] with the same replace semantics as the Room one. */
class InMemoryListingCache : ListingCache {
    private val listings = mutableMapOf<Pair<String, String>, Listing>()
    private val nearby = mutableMapOf<String, List<String>>()
    private val byInterest = mutableMapOf<String, List<String>>()
    var broken = false

    private fun check() = check(!broken) { "cache unavailable" }

    override suspend fun replaceFeed(viewerId: String, feed: Feed) {
        check()
        (feed.nearby + feed.byInterest).forEach { listings[viewerId to it.id] = it }
        nearby[viewerId] = feed.nearby.map { it.id }
        byInterest[viewerId] = feed.byInterest.map { it.id }
    }

    override suspend fun feed(viewerId: String): Feed? {
        check()
        val near = nearby[viewerId].orEmpty().mapNotNull { listings[viewerId to it] }
        val interest = byInterest[viewerId].orEmpty().mapNotNull { listings[viewerId to it] }
        return if (near.isEmpty() && interest.isEmpty()) null else Feed(near, interest)
    }

    override suspend fun replaceOwn(viewerId: String, status: ListingStatus, listings: List<Listing>) {
        check()
        this.listings.entries.removeAll { (key, value) ->
            key.first == viewerId && value.owner.id == viewerId && value.status == status
        }
        listings.forEach { this.listings[viewerId to it.id] = it }
    }

    override suspend fun own(viewerId: String, status: ListingStatus): List<Listing> {
        check()
        return listings.filter { (key, value) ->
            key.first == viewerId && value.owner.id == viewerId && value.status == status
        }.values.sortedByDescending { it.updatedAt }
    }

    override suspend fun put(viewerId: String, listing: Listing) {
        check()
        listings[viewerId to listing.id] = listing
    }

    override suspend fun listing(viewerId: String, id: String): Listing? {
        check()
        return listings[viewerId to id]
    }

    override suspend fun remove(viewerId: String, id: String) {
        check()
        listings.remove(viewerId to id)
        nearby[viewerId] = nearby[viewerId].orEmpty() - id
        byInterest[viewerId] = byInterest[viewerId].orEmpty() - id
    }
}

class FakeSessionStore(initial: Session?) : SessionStore {
    override val session = MutableStateFlow(initial)

    override suspend fun save(session: Session) {
        this.session.value = session
    }

    override suspend fun clear() {
        session.value = null
    }
}

/** The mock servers, the Ktor client and the repository wired as in the app, with a switchable connection. */
class ItemsHarness(
    user: String = "ana",
    val storage: MockStorage = InMemoryMockStorage(),
    /** The name the auth server keeps for each account; it may differ from the name the client holds. */
    accountNames: Map<String, String> = emptyMap(),
) {
    val sessions = FakeSessionStore(sessionOf(user))
    val cache = InMemoryListingCache()
    var offline = false
    private val profileServer = MockProfileServer(storage, MockSessions { it })
    val server = MockItemsServer(
        storage,
        MockSessions { it },
        Clock.systemUTC(),
        MockUserNames { id -> accountNames[id] ?: id.replaceFirstChar { it.uppercase() } },
        ViewerProfiles(profileServer::profileOf),
    )
    private val client = minkaHttpClient(
        MockApi(server.routes() + profileServer.routes(), Duration.ZERO, MockConnection { offline }).engine(),
    )
    val repository = KtorItemsRepository(client, sessions, cache)
    val profiles = KtorProfileRepository(client, sessions)

    suspend fun signInAs(user: String) = sessions.save(sessionOf(user))

    suspend fun publishedDraft(title: String = "Platos azules"): Listing =
        repository.publish(repository.saveDraft(null, completeDraft(title)).value().id).value()

    companion object {
        fun sessionOf(user: String) =
            Session(user, User(user, user.replaceFirstChar { it.uppercase() }, "$user@example.com"))

        fun completeDraft(title: String = "Platos azules") = ListingDraft(
            photos = listOf("photo-1", "photo-2"),
            title = title,
            category = Category.HOME,
            condition = Condition.GOOD,
            wants = listOf(Category.BOOKS),
            district = "Miraflores",
        )
    }
}

fun <T> ItemsOutcome<T>.value(): T = (this as Outcome.Success).value
