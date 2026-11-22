package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.core.domain.onSuccess
import com.trinitybytes.minka.core.network.ApiError
import com.trinitybytes.minka.core.network.ktorCall
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.ItemsOutcome
import com.trinitybytes.minka.features.items.domain.ItemsRepository
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.Loaded
import com.trinitybytes.minka.features.items.domain.OwnerProfile
import com.trinitybytes.minka.features.items.domain.SavedDraft
import com.trinitybytes.minka.features.items.domain.SearchQuery
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Items over the Ktor client. Reads that feed Inicio, the detail and "Mis publicaciones" are network-first:
 * a success refreshes the [ListingCache], and when the device is offline the cache answers and says so
 * through [Loaded.fromCache] (Figma 2.05). Writes need the network and keep the cache in step.
 */
class KtorItemsRepository @Inject constructor(
    private val client: HttpClient,
    private val sessions: SessionStore,
    private val cache: ListingCache,
) : ItemsRepository {

    override suspend fun feed(): ItemsOutcome<Loaded<Feed>> = withSession { session ->
        val viewer = session.user.id
        networkFirst(
            fetch = { get<FeedDto, Feed>(session, "feed", FeedDto::toDomain) },
            save = { cache.replaceFeed(viewer, it) },
            cached = { cache.feed(viewer) },
        )
    }

    override suspend fun detail(id: String): ItemsOutcome<Loaded<Listing>> = withSession { session ->
        val viewer = session.user.id
        networkFirst(
            fetch = { get<ListingDto, Listing>(session, "listings/$id", ListingDto::toDomain) },
            save = { cache.put(viewer, it) },
            cached = { cache.listing(viewer, id) },
        )
    }

    override suspend fun myListings(status: ListingStatus): ItemsOutcome<Loaded<List<Listing>>> =
        withSession { session ->
            val viewer = session.user.id
            networkFirst(
                fetch = {
                    get<List<ListingDto>, List<Listing>>(session, "me/listings/${status.name.lowercase()}") { list ->
                        list.map(ListingDto::toDomain)
                    }
                },
                save = { cache.replaceOwn(viewer, status, it) },
                cached = { cache.own(viewer, status) },
            )
        }

    override suspend fun search(query: SearchQuery): ItemsOutcome<List<Listing>> = withSession { session ->
        send<List<ListingDto>, List<Listing>>(session, { list -> list.map(ListingDto::toDomain) }) {
            post(session, "listings/search", query.toRequest())
        }
    }

    override suspend fun suggestions(text: String): ItemsOutcome<List<String>> = withSession { session ->
        send<List<String>, List<String>>(session, { it }) { post(session, "listings/suggestions", TextRequest(text)) }
    }

    override suspend fun recentSearches(): ItemsOutcome<List<String>> = withSession { session ->
        get<List<String>, List<String>>(session, "me/recent-searches") { it }
    }

    override suspend fun addRecentSearch(text: String): ItemsOutcome<List<String>> = withSession { session ->
        send<List<String>, List<String>>(session, { it }) { post(session, "me/recent-searches", TextRequest(text)) }
    }

    override suspend fun clearRecentSearches(): ItemsOutcome<Unit> = withSession { session ->
        send<Boolean, Unit>(session, { }) { client.delete("me/recent-searches") { authorize(session) } }
    }

    override suspend fun myDrafts(): ItemsOutcome<List<SavedDraft>> = withSession { session ->
        get<List<SavedDraftDto>, List<SavedDraft>>(session, "me/drafts") { list ->
            list.map(SavedDraftDto::toDomain)
        }
    }

    override suspend fun saveDraft(id: String?, draft: ListingDraft): ItemsOutcome<SavedDraft> =
        withSession { session ->
            val body = SaveDraftRequest(draft.toDto())
            send<SavedDraftDto, SavedDraft>(session, SavedDraftDto::toDomain) {
                if (id == null) post(session, "me/drafts", body) else put(session, "me/drafts/$id", body)
            }
        }

    override suspend fun draft(id: String): ItemsOutcome<SavedDraft> = withSession { session ->
        get<SavedDraftDto, SavedDraft>(session, "me/drafts/$id", SavedDraftDto::toDomain)
    }

    override suspend fun publish(id: String): ItemsOutcome<Listing> = withSession { session ->
        send<ListingDto, Listing>(session, ListingDto::toDomain) {
            client.post("me/drafts/$id/publication") { authorize(session) }
        }
            .onSuccess { saveToCache { cache.put(session.user.id, it) } }
    }

    override suspend fun setStatus(id: String, status: ListingStatus): ItemsOutcome<Listing> =
        withSession { session ->
            send<ListingDto, Listing>(session, ListingDto::toDomain) {
                put(session, "me/listings/$id/status", StatusRequest(status.name))
            }.onSuccess { saveToCache { cache.put(session.user.id, it) } }
        }

    override suspend fun delete(id: String): ItemsOutcome<Unit> = withSession { session ->
        send<Boolean, Unit>(session, { }) { client.delete("me/listings/$id") { authorize(session) } }
            .onSuccess { saveToCache { cache.remove(session.user.id, id) } }
    }

    override suspend fun hide(id: String): ItemsOutcome<Unit> = withSession { session ->
        send<Boolean, Unit>(session, { }) { client.put("me/hidden/$id") { authorize(session) } }
            .onSuccess { saveToCache { cache.remove(session.user.id, id) } }
    }

    override suspend fun unhide(id: String): ItemsOutcome<Unit> = withSession { session ->
        send<Boolean, Unit>(session, { }) { client.delete("me/hidden/$id") { authorize(session) } }
    }

    override suspend fun ownerProfile(id: String): ItemsOutcome<OwnerProfile> = withSession { session ->
        get<OwnerProfileDto, OwnerProfile>(session, "owners/$id", OwnerProfileDto::toDomain)
    }

    private suspend fun <T> withSession(block: suspend (Session) -> ItemsOutcome<T>): ItemsOutcome<T> {
        val session = sessions.session.first() ?: return Outcome.Failure(ItemsError.Unauthorized)
        return block(session)
    }

    /** Asks the network; on success refreshes the cache, and offline falls back to what the cache holds. */
    private suspend fun <T> networkFirst(
        fetch: suspend () -> ItemsOutcome<T>,
        save: suspend (T) -> Unit,
        cached: suspend () -> T?,
    ): ItemsOutcome<Loaded<T>> = when (val result = fetch()) {
        is Outcome.Success -> {
            saveToCache { save(result.value) }
            Outcome.Success(Loaded(result.value))
        }
        is Outcome.Failure -> {
            val saved = if (result.error == ItemsError.Offline) readCache(cached) else null
            if (saved != null) Outcome.Success(Loaded(saved, fromCache = true)) else result
        }
    }

    /** A broken cache must never fail a request that otherwise worked. */
    private suspend fun saveToCache(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun <T> readCache(read: suspend () -> T?): T? = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend inline fun <reified D, T> get(
        session: Session,
        path: String,
        crossinline toDomain: (D) -> T,
    ): ItemsOutcome<T> = send(session, toDomain) { client.get(path) { authorize(session) } }

    private suspend inline fun <reified D, T> send(
        session: Session,
        crossinline toDomain: (D) -> T,
        crossinline request: suspend () -> HttpResponse,
    ): ItemsOutcome<T> = ktorCall(
        toDomain,
        { it.body<ApiError>().toItemsError() },
        ItemsError.Offline,
        ItemsError.Unexpected,
        { request() },
    )

    private suspend inline fun <reified B> post(session: Session, path: String, body: B): HttpResponse =
        client.post(path) {
            authorize(session)
            json(body)
        }

    private suspend inline fun <reified B> put(session: Session, path: String, body: B): HttpResponse =
        client.put(path) {
            authorize(session)
            json(body)
        }

    private fun HttpRequestBuilder.authorize(session: Session) {
        header(HttpHeaders.Authorization, "Bearer ${session.token}")
    }

    private inline fun <reified B> HttpRequestBuilder.json(body: B) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun ApiError.toItemsError(): ItemsError = when (code) {
        ItemsErrorCodes.UNAUTHORIZED -> ItemsError.Unauthorized
        ItemsErrorCodes.NOT_FOUND -> ItemsError.NotFound
        ItemsErrorCodes.INVALID_DRAFT, ItemsErrorCodes.INVALID_STATUS -> ItemsError.Invalid
        else -> ItemsError.Unexpected
    }
}
