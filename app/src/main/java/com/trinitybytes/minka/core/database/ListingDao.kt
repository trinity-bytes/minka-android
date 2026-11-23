package com.trinitybytes.minka.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(listings: List<ListingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedEntries(entries: List<FeedEntryEntity>)

    @Query("SELECT * FROM listings WHERE viewerId = :viewerId AND id = :id")
    suspend fun listing(viewerId: String, id: String): ListingEntity?

    /** The listings of one shelf of Inicio, in the order the server ranked them. */
    @Query(
        "SELECT l.* FROM listings AS l " +
            "INNER JOIN feed_entries AS f ON f.viewerId = l.viewerId AND f.listingId = l.id " +
            "WHERE f.viewerId = :viewerId AND f.section = :section ORDER BY f.position",
    )
    suspend fun feed(viewerId: String, section: String): List<ListingEntity>

    /** The viewer's own listings with one status, most recently changed first (Figma 11.05). */
    @Query(
        "SELECT * FROM listings WHERE viewerId = :viewerId AND ownerId = :viewerId AND status = :status " +
            "ORDER BY updatedAt DESC",
    )
    suspend fun own(viewerId: String, status: String): List<ListingEntity>

    @Query("DELETE FROM feed_entries WHERE viewerId = :viewerId")
    suspend fun clearFeed(viewerId: String)

    @Query("DELETE FROM feed_entries WHERE viewerId = :viewerId AND listingId = :id")
    suspend fun removeFromFeed(viewerId: String, id: String)

    /** Drops other people's listings that no shelf of the viewer's feed refers to any more. */
    @Query(
        "DELETE FROM listings WHERE viewerId = :viewerId AND ownerId <> :viewerId " +
            "AND id NOT IN (SELECT listingId FROM feed_entries WHERE viewerId = :viewerId)",
    )
    suspend fun pruneForeign(viewerId: String)

    /** Drops the viewer's own listings with [status] that the server no longer returns. */
    @Query(
        "DELETE FROM listings WHERE viewerId = :viewerId AND ownerId = :viewerId AND status = :status " +
            "AND id NOT IN (:keepIds)",
    )
    suspend fun pruneOwn(viewerId: String, status: String, keepIds: List<String>)

    @Query("DELETE FROM listings WHERE viewerId = :viewerId AND id = :id")
    suspend fun delete(viewerId: String, id: String)
}
