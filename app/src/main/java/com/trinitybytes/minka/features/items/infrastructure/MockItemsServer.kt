package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.core.network.MockCall
import com.trinitybytes.minka.core.network.MockDocument
import com.trinitybytes.minka.core.network.MockReply
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockSessions
import com.trinitybytes.minka.core.network.MockUserNames
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.SearchSort
import com.trinitybytes.minka.features.items.domain.validateReview
import com.trinitybytes.minka.features.onboarding.infrastructure.MockDistricts
import com.trinitybytes.minka.features.onboarding.infrastructure.ProfileDto
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.time.Clock
import java.util.UUID
import kotlinx.serialization.Serializable

/** What the items server needs to know about the viewer to personalize feed and distances. */
fun interface ViewerProfiles {
    suspend fun of(userId: String): ProfileDto
}

/**
 * The items half of the in-app mock API: listings, drafts, search, hidden items and recent searches
 * (Figma flows 02-05 and 11). Everything is persisted in [MockStorage]; ownership is tied to the user id
 * that [MockSessions] resolves, so nobody can change, publish or delete another user's listing.
 */
class MockItemsServer(
    storage: MockStorage,
    private val sessions: MockSessions,
    private val clock: Clock,
    private val names: MockUserNames,
    private val profiles: ViewerProfiles,
) {
    private val document = MockDocument(storage, STORAGE_KEY, ItemsDb.serializer()) { ItemsDb() }

    fun routes(): Set<MockRoute> = setOf(
        route(HttpMethod.Get, "feed") { _, user -> feed(user) },
        route(HttpMethod.Post, "listings/search") { call, user -> search(user, call.body()) },
        route(HttpMethod.Post, "listings/suggestions") { call, user ->
            suggestions(user, call.body<TextRequest>().text)
        },
        route(HttpMethod.Get, "listings/{id}") { call, user -> detail(user, call.pathParam("id")) },
        route(HttpMethod.Get, "owners/{id}") { call, user -> ownerProfile(user, call.pathParam("id")) },
        route(HttpMethod.Get, "me/listings/{status}") { call, user -> mine(user, call.pathParam("status")) },
        route(HttpMethod.Put, "me/listings/{id}/status") { call, user ->
            setStatus(user, call.pathParam("id"), call.body<StatusRequest>().status)
        },
        route(HttpMethod.Delete, "me/listings/{id}") { call, user -> delete(user, call.pathParam("id")) },
        route(HttpMethod.Get, "me/drafts") { _, user -> drafts(user) },
        route(HttpMethod.Post, "me/drafts") { call, user -> saveDraft(user, null, call.body()) },
        route(HttpMethod.Get, "me/drafts/{id}") { call, user -> draft(user, call.pathParam("id")) },
        route(HttpMethod.Put, "me/drafts/{id}") { call, user -> saveDraft(user, call.pathParam("id"), call.body()) },
        route(HttpMethod.Post, "me/drafts/{id}/publication") { call, user -> publish(user, call.pathParam("id")) },
        route(HttpMethod.Put, "me/hidden/{id}") { call, user -> setHidden(user, call.pathParam("id"), true) },
        route(HttpMethod.Delete, "me/hidden/{id}") { call, user -> setHidden(user, call.pathParam("id"), false) },
        route(HttpMethod.Get, "me/recent-searches") { _, user -> recentSearches(user) },
        route(HttpMethod.Post, "me/recent-searches") { call, user ->
            addRecentSearch(user, call.body<TextRequest>().text)
        },
        route(HttpMethod.Delete, "me/recent-searches") { _, user -> clearRecentSearches(user) },
    )

    private fun route(method: HttpMethod, path: String, handler: suspend (MockCall, String) -> MockReply) =
        MockRoute(method, path) { call ->
            val user = call.bearerToken()?.let { sessions.userFor(it) }
            if (user == null) reply(HttpStatusCode.Unauthorized, ItemsErrorCodes.UNAUTHORIZED) else handler(call, user)
        }

    private suspend fun viewerDistrict(user: String): String? = profiles.of(user).district?.name

    private suspend fun feed(user: String): MockReply {
        val profile = profiles.of(user)
        val district = profile.district?.name
        val interests = profile.interestIds.mapNotNull(INTEREST_CATEGORIES::get).toSet()
        return document.read { db ->
            val visible = db.visibleTo(user).mapNotNull { it.toDto(district) }
            val nearby = visible.sortedBy { it.distanceKm ?: Double.POSITIVE_INFINITY }
            val byInterest = visible.filter { it.category in interests }
                .sortedByDescending { it.createdAt }
            MockReply.json(HttpStatusCode.OK, FeedDto(nearby, byInterest))
        }
    }

    private suspend fun search(user: String, request: SearchRequest): MockReply {
        val district = viewerDistrict(user)
        val text = request.text.trim()
        val condition = request.condition?.let(Condition::valueOf)
        return document.read { db ->
            val matches = db.visibleTo(user).filter { item ->
                (text.isEmpty() || item.draft.matches(text)) &&
                    (request.category == null || item.draft.category == request.category) &&
                    (condition == null || item.draft.condition?.let { Condition.valueOf(it) <= condition } == true)
            }.mapNotNull { it.toDto(district) }.filter { item ->
                request.distanceKm == null || (item.distanceKm != null && item.distanceKm <= request.distanceKm)
            }
            val sorted = when (SearchSort.valueOf(request.sort)) {
                SearchSort.NEWEST -> matches.sortedByDescending { it.createdAt }
                SearchSort.NEAREST -> matches.sortedBy { it.distanceKm ?: Double.POSITIVE_INFINITY }
            }
            MockReply.json(HttpStatusCode.OK, sorted)
        }
    }

    private suspend fun suggestions(user: String, text: String): MockReply = document.read { db ->
        val titles = if (text.isBlank()) {
            emptyList()
        } else {
            db.visibleTo(user).map { it.draft.title }.filter { it.contains(text.trim(), ignoreCase = true) }.distinct()
        }
        MockReply.json(HttpStatusCode.OK, titles.take(MAX_SUGGESTIONS))
    }

    private suspend fun detail(user: String, id: String): MockReply {
        val district = viewerDistrict(user)
        return document.read { db ->
            val item = db.listings.find { it.id == id && it.status != StoredStatus.DRAFT }
                ?.takeIf { it.ownerId == user || id !in db.hidden[user].orEmpty() }
            item?.toDto(district)?.let { MockReply.json(HttpStatusCode.OK, it) } ?: notFound()
        }
    }

    private suspend fun ownerProfile(user: String, ownerId: String): MockReply {
        val district = viewerDistrict(user)
        return document.read { db ->
            val published = db.listings.filter { it.ownerId == ownerId && it.status != StoredStatus.DRAFT }
            val owner = published.firstOrNull()?.owner() ?: return@read notFound()
            val active = published.filter { it.status == StoredStatus.ACTIVE && it.id !in db.hidden[user].orEmpty() }
            MockReply.json(HttpStatusCode.OK, OwnerProfileDto(owner, active.mapNotNull { it.toDto(district) }))
        }
    }

    private suspend fun mine(user: String, statusPath: String): MockReply {
        val status = StoredStatus.entries.firstOrNull { it.name.equals(statusPath, ignoreCase = true) }
        if (status == null || status == StoredStatus.DRAFT) return notFound()
        val district = viewerDistrict(user)
        return document.read { db ->
            val own = db.listings.filter { it.ownerId == user && it.status == status }
                .sortedByDescending { it.updatedAt }
            MockReply.json(HttpStatusCode.OK, own.mapNotNull { it.toDto(district) })
        }
    }

    private suspend fun drafts(user: String): MockReply = document.read { db ->
        val own = db.listings.filter { it.ownerId == user && it.status == StoredStatus.DRAFT }
            .sortedByDescending { it.updatedAt }
        MockReply.json(HttpStatusCode.OK, own.map { it.toDraftDto() })
    }

    private suspend fun draft(user: String, id: String): MockReply = document.read { db ->
        db.draftOf(user, id)?.let { MockReply.json(HttpStatusCode.OK, it.toDraftDto()) } ?: notFound()
    }

    private suspend fun saveDraft(user: String, id: String?, body: SaveDraftRequest): MockReply {
        val ownerName = names.nameOf(user) ?: return MockReply.error(HttpStatusCode.Unauthorized, "unauthorized")
        return document.update { db ->
            val old = id?.let { db.draftOf(user, it) ?: return@update notFound() }
            val now = clock.millis()
            val saved = StoredListing(
                id = old?.id ?: UUID.randomUUID().toString(),
                ownerId = user,
                ownerName = old?.ownerName ?: ownerName,
                ownerSince = old?.ownerSince,
                status = StoredStatus.DRAFT,
                draft = body.draft,
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
            )
            db.listings.removeAll { it.id == saved.id }
            db.listings.add(saved)
            MockReply.json(HttpStatusCode.OK, saved.toDraftDto())
        }
    }

    private suspend fun publish(user: String, id: String): MockReply = document.update { db ->
        val old = db.draftOf(user, id) ?: return@update notFound()
        if (old.draft.toDomain().validateReview().isNotEmpty()) {
            return@update reply(HttpStatusCode.UnprocessableEntity, ItemsErrorCodes.INVALID_DRAFT)
        }
        val published = old.copy(status = StoredStatus.ACTIVE, updatedAt = clock.millis())
        db.listings[db.listings.indexOfFirst { it.id == id }] = published
        MockReply.json(HttpStatusCode.OK, published.toDto(published.draft.district) ?: return@update notFound())
    }

    private suspend fun setStatus(user: String, id: String, statusName: String): MockReply {
        val status = StoredStatus.entries.firstOrNull { it.name == statusName && it != StoredStatus.DRAFT }
            ?: return reply(HttpStatusCode.UnprocessableEntity, ItemsErrorCodes.INVALID_STATUS)
        val district = viewerDistrict(user)
        return document.update { db ->
            val index = db.listings.indexOfFirst {
                it.id == id && it.ownerId == user && it.status != StoredStatus.DRAFT
            }
            if (index < 0) return@update notFound()
            val changed = db.listings[index].copy(status = status, updatedAt = clock.millis())
            db.listings[index] = changed
            changed.toDto(district)?.let { MockReply.json(HttpStatusCode.OK, it) } ?: notFound()
        }
    }

    private suspend fun delete(user: String, id: String): MockReply = document.update { db ->
        if (db.listings.removeAll { it.id == id && it.ownerId == user }) ok() else notFound()
    }

    private suspend fun setHidden(user: String, id: String, hidden: Boolean): MockReply = document.update { db ->
        if (hidden) {
            val target = db.listings.find { it.id == id && it.ownerId != user && it.status != StoredStatus.DRAFT }
            if (target == null) return@update notFound()
        }
        val ids = db.hidden[user].orEmpty().toMutableSet()
        if (hidden) ids.add(id) else ids.remove(id)
        db.hidden[user] = ids.toList()
        ok()
    }

    private suspend fun recentSearches(user: String): MockReply = document.read { db ->
        MockReply.json(HttpStatusCode.OK, db.recent[user].orEmpty())
    }

    private suspend fun addRecentSearch(user: String, text: String): MockReply = document.update { db ->
        val value = text.trim()
        val old = db.recent[user].orEmpty()
        val next = if (value.isEmpty()) old else (listOf(value) + old.filterNot { it.equals(value, true) })
        db.recent[user] = next.take(MAX_RECENT)
        MockReply.json(HttpStatusCode.OK, db.recent[user].orEmpty())
    }

    private suspend fun clearRecentSearches(user: String): MockReply = document.update { db ->
        db.recent[user] = emptyList()
        ok()
    }

    private fun ok() = MockReply.json(HttpStatusCode.OK, true)

    private fun notFound() = reply(HttpStatusCode.NotFound, ItemsErrorCodes.NOT_FOUND)

    private fun reply(status: HttpStatusCode, code: String) = MockReply.error(status, code)

    companion object {
        private const val STORAGE_KEY = "items"
        private const val MAX_RECENT = 10
        private const val MAX_SUGGESTIONS = 10

        /** The interests of Figma 1.19 that match a listing category; the others have no listings yet. */
        private val INTEREST_CATEGORIES = mapOf(
            "hogar" to Category.HOME.name,
            "ropa" to Category.CLOTHING.name,
            "libros" to Category.BOOKS.name,
            "tecnologia" to Category.TECHNOLOGY.name,
            "deportes" to Category.SPORTS.name,
            "herramientas" to Category.TOOLS.name,
            "plantas" to Category.PLANTS.name,
            "servicios" to Category.SERVICES.name,
        )
    }
}

@Serializable
internal enum class StoredStatus { DRAFT, ACTIVE, PAUSED }

/** A listing or draft as the mock server keeps it. */
@Serializable
internal data class StoredListing(
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val ownerSince: Long? = null,
    val status: StoredStatus,
    val draft: DraftDto,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun owner() = OwnerDto(ownerId, ownerName, draft.district, ownerSince)

    fun toDraftDto() = SavedDraftDto(id, draft, updatedAt)

    /** Null when the record is not a complete listing, which only a draft can be. */
    fun toDto(viewerDistrict: String?): ListingDto? {
        val category = draft.category ?: return null
        val condition = draft.condition ?: return null
        if (draft.photos.isEmpty() || status == StoredStatus.DRAFT) return null
        return ListingDto(
            id = id,
            photos = draft.photos,
            title = draft.title,
            category = category,
            condition = condition,
            wants = draft.wants,
            district = draft.district,
            status = status.name,
            owner = owner(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            description = draft.description,
            estimatedValue = draft.estimatedValue,
            meetingPoint = draft.meetingPoint,
            hideExactLocation = draft.hideExactLocation,
            homeDelivery = draft.homeDelivery,
            distanceKm = MockDistricts.distanceKm(viewerDistrict, draft.district),
        )
    }
}

/** Whether the title or description contains [text], ignoring case. */
private fun DraftDto.matches(text: String) =
    title.contains(text, ignoreCase = true) || description.orEmpty().contains(text, ignoreCase = true)

@Serializable
internal data class ItemsDb(
    val listings: MutableList<StoredListing> = seedListings().toMutableList(),
    val hidden: MutableMap<String, List<String>> = mutableMapOf(),
    val recent: MutableMap<String, List<String>> = mutableMapOf(),
) {
    /** Active listings of other people that [user] has not hidden: what feed and search draw from. */
    fun visibleTo(user: String): List<StoredListing> = listings.filter {
        it.status == StoredStatus.ACTIVE && it.ownerId != user && it.id !in hidden[user].orEmpty()
    }

    fun draftOf(user: String, id: String): StoredListing? =
        listings.find { it.id == id && it.ownerId == user && it.status == StoredStatus.DRAFT }
}
