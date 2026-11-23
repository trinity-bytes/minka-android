package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus

/**
 * Port to the device cache behind [KtorItemsRepository]. Everything is scoped to [viewerId], the signed-in
 * user, because distances and visibility differ per viewer.
 */
interface ListingCache {
    /** Replaces both shelves of the viewer's Inicio. */
    suspend fun replaceFeed(viewerId: String, feed: Feed)

    /** The last saved Inicio, or null when none was saved or it was empty. */
    suspend fun feed(viewerId: String): Feed?

    /** Replaces the viewer's own listings with [status] by what the server just returned. */
    suspend fun replaceOwn(viewerId: String, status: ListingStatus, listings: List<Listing>)

    suspend fun own(viewerId: String, status: ListingStatus): List<Listing>

    suspend fun put(viewerId: String, listing: Listing)

    suspend fun listing(viewerId: String, id: String): Listing?

    /** Forgets a listing everywhere: it was deleted or the viewer hid it. */
    suspend fun remove(viewerId: String, id: String)
}
