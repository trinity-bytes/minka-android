package com.trinitybytes.minka.features.items.infrastructure

import androidx.room.withTransaction
import com.trinitybytes.minka.core.database.FeedEntryEntity
import com.trinitybytes.minka.core.database.ItemsDatabase
import com.trinitybytes.minka.core.database.ListingEntity
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.Listing
import com.trinitybytes.minka.features.items.domain.ListingStatus
import com.trinitybytes.minka.features.items.domain.OwnerSummary
import java.time.Instant
import javax.inject.Inject

/** [ListingCache] on Room. Every call is a suspend function, so Room runs it off the main thread. */
class RoomListingCache @Inject constructor(private val database: ItemsDatabase) : ListingCache {
    private val dao = database.listings()

    override suspend fun replaceFeed(viewerId: String, feed: Feed) = database.withTransaction {
        dao.clearFeed(viewerId)
        dao.upsert((feed.nearby + feed.byInterest).map { it.toEntity(viewerId) })
        dao.insertFeedEntries(feed.nearby.entries(viewerId, NEARBY) + feed.byInterest.entries(viewerId, BY_INTEREST))
        dao.pruneForeign(viewerId)
    }

    override suspend fun feed(viewerId: String): Feed? {
        val nearby = dao.feed(viewerId, NEARBY).mapNotNull { it.toDomain() }
        val byInterest = dao.feed(viewerId, BY_INTEREST).mapNotNull { it.toDomain() }
        return if (nearby.isEmpty() && byInterest.isEmpty()) null else Feed(nearby, byInterest)
    }

    override suspend fun replaceOwn(viewerId: String, status: ListingStatus, listings: List<Listing>) =
        database.withTransaction {
            dao.upsert(listings.map { it.toEntity(viewerId) })
            dao.pruneOwn(viewerId, status.name, listings.map { it.id })
        }

    override suspend fun own(viewerId: String, status: ListingStatus): List<Listing> =
        dao.own(viewerId, status.name).mapNotNull { it.toDomain() }

    override suspend fun put(viewerId: String, listing: Listing) = dao.upsert(listOf(listing.toEntity(viewerId)))

    override suspend fun listing(viewerId: String, id: String): Listing? = dao.listing(viewerId, id)?.toDomain()

    override suspend fun remove(viewerId: String, id: String) = database.withTransaction {
        dao.removeFromFeed(viewerId, id)
        dao.delete(viewerId, id)
    }

    private fun List<Listing>.entries(viewerId: String, section: String) =
        mapIndexed { position, listing -> FeedEntryEntity(viewerId, section, listing.id, position) }

    private companion object {
        const val NEARBY = "NEARBY"
        const val BY_INTEREST = "BY_INTEREST"
    }
}

private fun Listing.toEntity(viewerId: String) = ListingEntity(
    viewerId = viewerId,
    id = id,
    ownerId = owner.id,
    ownerName = owner.name,
    ownerDistrict = owner.district,
    ownerMemberSince = owner.memberSince?.toEpochMilli(),
    photos = photos,
    title = title,
    category = category.name,
    condition = condition.name,
    wants = wants.map(Category::name),
    description = description,
    estimatedValue = estimatedValue,
    district = district,
    meetingPoint = meetingPoint,
    hideExactLocation = hideExactLocation,
    homeDelivery = homeDelivery,
    status = status.name,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    distanceKm = distanceKm,
)

/** Null for a row without photos, which cannot be a listing; such a row is skipped rather than shown broken. */
private fun ListingEntity.toDomain(): Listing? {
    val cover = photos.firstOrNull() ?: return null
    return Listing(
        id = id,
        cover = cover,
        morePhotos = photos.drop(1),
        title = title,
        category = Category.valueOf(category),
        condition = Condition.valueOf(condition),
        wants = wants.map(Category::valueOf),
        district = district,
        status = ListingStatus.valueOf(status),
        owner = OwnerSummary(ownerId, ownerName, ownerDistrict, ownerMemberSince?.let(Instant::ofEpochMilli)),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        description = description,
        estimatedValue = estimatedValue,
        meetingPoint = meetingPoint,
        hideExactLocation = hideExactLocation,
        homeDelivery = homeDelivery,
        distanceKm = distanceKm,
    )
}
