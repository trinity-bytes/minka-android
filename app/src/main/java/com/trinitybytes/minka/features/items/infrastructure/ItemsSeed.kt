package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition

private const val PHOTO_BASE = "android.resource://com.trinitybytes.minka/drawable/"
private const val DEMO_USER_ID = "user-ana"
private const val DEMO_USER_NAME = "Ana Quispe"
private const val LUCIA_MEMBER_SINCE = 1672531200000L
private const val SEED_CREATED = 1720000000000L

/**
 * The listings every fresh install starts with: one from another neighbor, and a published, a paused and a
 * draft listing for the demo account `user-ana`, so every tab of "Mis publicaciones" has content.
 */
internal fun seedListings(): List<StoredListing> = listOf(
    StoredListing(
        id = "seed-plates",
        ownerId = "seed-lucia",
        ownerName = "Lucía Torres",
        ownerSince = LUCIA_MEMBER_SINCE,
        status = StoredStatus.ACTIVE,
        draft = DraftDto(
            photos = listOf(PHOTO_BASE + "listing_seed_plates"),
            title = "Juego de platos de cerámica",
            category = Category.HOME.name,
            condition = Condition.GOOD.name,
            wants = listOf(Category.BOOKS.name),
            description = "Seis platos hondos pintados a mano.",
            district = "Miraflores",
            meetingPoint = "Parque Kennedy",
        ),
        createdAt = SEED_CREATED,
        updatedAt = SEED_CREATED,
    ),
    StoredListing(
        id = "seed-ana-bike",
        ownerId = DEMO_USER_ID,
        ownerName = DEMO_USER_NAME,
        status = StoredStatus.ACTIVE,
        draft = DraftDto(
            photos = listOf(PHOTO_BASE + "listing_seed_bicycle"),
            title = "Bicicleta de montaña",
            category = Category.SPORTS.name,
            condition = Condition.GOOD.name,
            wants = listOf(Category.BOOKS.name),
            district = "Miraflores",
        ),
        createdAt = SEED_CREATED,
        updatedAt = SEED_CREATED,
    ),
    StoredListing(
        id = "seed-ana-table",
        ownerId = DEMO_USER_ID,
        ownerName = DEMO_USER_NAME,
        status = StoredStatus.PAUSED,
        draft = DraftDto(
            photos = listOf(PHOTO_BASE + "listing_seed_table"),
            title = "Mesa de centro",
            category = Category.HOME.name,
            condition = Condition.USED.name,
            wants = listOf(Category.PLANTS.name),
            district = "Miraflores",
        ),
        createdAt = 1710000000000L,
        updatedAt = 1710000000000L,
    ),
    StoredListing(
        id = "seed-ana-draft",
        ownerId = DEMO_USER_ID,
        ownerName = DEMO_USER_NAME,
        status = StoredStatus.DRAFT,
        draft = DraftDto(
            photos = listOf(PHOTO_BASE + "listing_seed_plates"),
            title = "Juego de platos de cerámica",
            category = Category.HOME.name,
            district = "Miraflores",
        ),
        createdAt = 1730000000000L,
        updatedAt = 1730000000000L,
    ),
)
