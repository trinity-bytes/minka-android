package com.trinitybytes.minka.core.database

import androidx.room.Entity
import androidx.room.Index

/**
 * One listing as a signed-in user last saw it. The viewer is part of the key because distance and
 * visibility depend on who looks. Enums are stored by name and times as epoch milliseconds, so this table
 * does not depend on the feature's domain types.
 */
@Entity(
    tableName = "listings",
    primaryKeys = ["viewerId", "id"],
    indices = [Index(value = ["viewerId", "ownerId", "status"])],
)
data class ListingEntity(
    val viewerId: String,
    val id: String,
    val ownerId: String,
    val ownerName: String,
    val ownerDistrict: String,
    val ownerMemberSince: Long?,
    /** Gallery order; the first entry is the cover. */
    val photos: List<String>,
    val title: String,
    val category: String,
    val condition: String,
    val wants: List<String>,
    val description: String?,
    val estimatedValue: Long?,
    val district: String,
    val meetingPoint: String?,
    val hideExactLocation: Boolean,
    val homeDelivery: Boolean,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val distanceKm: Double?,
)

/** Which listings a viewer's Inicio shows in each shelf, and in what order. */
@Entity(
    tableName = "feed_entries",
    primaryKeys = ["viewerId", "section", "listingId"],
    indices = [Index(value = ["viewerId", "section", "position"])],
)
data class FeedEntryEntity(
    val viewerId: String,
    val section: String,
    val listingId: String,
    val position: Int,
)
