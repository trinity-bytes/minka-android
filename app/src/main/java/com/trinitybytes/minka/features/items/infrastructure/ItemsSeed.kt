package com.trinitybytes.minka.features.items.infrastructure

import com.trinitybytes.minka.features.auth.infrastructure.DEMO_USER_ID
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Condition

private const val PHOTO_BASE = "android.resource://com.trinitybytes.minka/drawable/"
private const val DEMO_USER_NAME = "Ana Quispe"
private const val LUCIA_MEMBER_SINCE = 1672531200000L
private const val SEED_CREATED = 1720000000000L
private const val DAY = 86_400_000L

private class Neighbor(val id: String, val name: String, val since: Long)

private val Lucia = Neighbor("seed-lucia", "Lucía Torres", LUCIA_MEMBER_SINCE)
private val Carlos = Neighbor("seed-carlos", "Carlos R.", 1680000000000L)
private val Rosa = Neighbor("seed-rosa", "Rosa Mendoza", 1690000000000L)
private val Diego = Neighbor("seed-diego", "Diego Paredes", 1700000000000L)
private val Maria = Neighbor("seed-maria", "María Q.", 1685000000000L)

@Suppress("LongParameterList")
private fun neighborListing(
    id: String,
    owner: Neighbor,
    photo: String,
    title: String,
    category: Category,
    condition: Condition,
    wants: Category,
    district: String,
    description: String,
    meetingPoint: String,
    daysAgo: Long,
) = StoredListing(
    id = id,
    ownerId = owner.id,
    ownerName = owner.name,
    ownerSince = owner.since,
    status = StoredStatus.ACTIVE,
    draft = DraftDto(
        photos = listOf(PHOTO_BASE + photo),
        title = title,
        category = category.name,
        condition = condition.name,
        wants = listOf(wants.name),
        description = description,
        district = district,
        meetingPoint = meetingPoint,
    ),
    createdAt = SEED_CREATED - daysAgo * DAY,
    updatedAt = SEED_CREATED - daysAgo * DAY,
)

/**
 * The listings every fresh install starts with: ten from five other neighbors across the districts near
 * Miraflores (photos from the Figma mockups), and a published, a paused and a draft listing for the demo
 * account `user-ana`, so every tab of "Mis publicaciones" has content.
 */
internal fun seedListings(): List<StoredListing> = neighbors() + demoAccountListings()

@Suppress("LongMethod")
private fun neighbors(): List<StoredListing> = listOf(
    neighborListing(
        id = "seed-plates",
        owner = Lucia,
        photo = "listing_seed_teacup",
        title = "Juego de platos de cerámica",
        category = Category.HOME,
        condition = Condition.GOOD,
        wants = Category.BOOKS,
        district = "Miraflores",
        description = "Seis platos hondos pintados a mano.",
        meetingPoint = "Parque Kennedy",
        daysAgo = 0,
    ),
    neighborListing(
        id = "seed-guitar",
        owner = Carlos,
        photo = "listing_seed_guitar",
        title = "Guitarra acústica",
        category = Category.HOME,
        condition = Condition.GOOD,
        wants = Category.PLANTS,
        district = "Barranco",
        description = "Cuerdas nuevas, suena muy bien. Incluye funda.",
        meetingPoint = "Parque Municipal de Barranco",
        daysAgo = 2,
    ),
    neighborListing(
        id = "seed-table",
        owner = Lucia,
        photo = "listing_seed_table",
        title = "Mesa de madera",
        category = Category.HOME,
        condition = Condition.GOOD,
        wants = Category.CLOTHING,
        district = "San Isidro",
        description = "Mesa redonda de madera maciza, ideal para sala o recibidor.",
        meetingPoint = "Parque El Olivar",
        daysAgo = 3,
    ),
    neighborListing(
        id = "seed-stories",
        owner = Rosa,
        photo = "listing_seed_books",
        title = "Libros de cuentos",
        category = Category.BOOKS,
        condition = Condition.GOOD,
        wants = Category.HOME,
        district = "Santiago de Surco",
        description = "Doce cuentos infantiles ilustrados, tapa dura.",
        meetingPoint = "Parque de la Amistad",
        daysAgo = 4,
    ),
    neighborListing(
        id = "seed-mtb",
        owner = Diego,
        photo = "listing_seed_bike",
        title = "Bicicleta de montaña aro 26",
        category = Category.SPORTS,
        condition = Condition.GOOD,
        wants = Category.BOOKS,
        district = "Lince",
        description = "Aro 26, 21 cambios y frenos revisados en setiembre. Ideal para paseos por el malecón.",
        meetingPoint = "Plaza Manco Cápac",
        daysAgo = 2,
    ),
    neighborListing(
        id = "seed-urban-bike",
        owner = Carlos,
        photo = "listing_seed_bike",
        title = "Bicicleta urbana",
        category = Category.SPORTS,
        condition = Condition.LIKE_NEW,
        wants = Category.TOOLS,
        district = "Barranco",
        description = "Cuadro de acero y sillín de cuero, casi sin uso.",
        meetingPoint = "Bajada de Baños",
        daysAgo = 6,
    ),
    neighborListing(
        id = "seed-saddle",
        owner = Diego,
        photo = "listing_seed_bike_detail",
        title = "Sillín y manubrio de cuero",
        category = Category.SPORTS,
        condition = Condition.USED,
        wants = Category.PLANTS,
        district = "Miraflores",
        description = "Juego de sillín y manubrio clásicos, listos para instalar.",
        meetingPoint = "Parque Kennedy",
        daysAgo = 5,
    ),
    neighborListing(
        id = "seed-novels",
        owner = Rosa,
        photo = "listing_seed_books",
        title = "Novelas clásicas en español",
        category = Category.BOOKS,
        condition = Condition.USED,
        wants = Category.PLANTS,
        district = "Santiago de Surco",
        description = "Cinco novelas de tapa blanda, con algunas anotaciones a lápiz.",
        meetingPoint = "Parque de la Amistad",
        daysAgo = 8,
    ),
    neighborListing(
        id = "seed-beginner-guitar",
        owner = Maria,
        photo = "listing_seed_guitar",
        title = "Guitarra para principiantes",
        category = Category.HOME,
        condition = Condition.USED,
        wants = Category.BOOKS,
        district = "Jesús María",
        description = "Guitarra criolla con cuerdas de nailon, perfecta para empezar.",
        meetingPoint = "Campo de Marte",
        daysAgo = 9,
    ),
    neighborListing(
        id = "seed-dining-table",
        owner = Maria,
        photo = "listing_seed_table",
        title = "Mesa auxiliar de madera",
        category = Category.HOME,
        condition = Condition.LIKE_NEW,
        wants = Category.CLOTHING,
        district = "Jesús María",
        description = "Mesa pequeña de madera oscura, con base metálica.",
        meetingPoint = "Campo de Marte",
        daysAgo = 10,
    ),
)

private fun demoAccountListings(): List<StoredListing> = listOf(
    StoredListing(
        id = "seed-ana-bike",
        ownerId = DEMO_USER_ID,
        ownerName = DEMO_USER_NAME,
        status = StoredStatus.ACTIVE,
        draft = DraftDto(
            photos = listOf(PHOTO_BASE + "listing_seed_bike"),
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
            photos = listOf(PHOTO_BASE + "listing_seed_teacup"),
            title = "Juego de platos de cerámica",
            category = Category.HOME.name,
            district = "Miraflores",
        ),
        createdAt = 1730000000000L,
        updatedAt = 1730000000000L,
    ),
)

/** The distinct photo URIs the seed uses, for checking they all resolve to a bundled drawable. */
internal fun seedPhotoUris(): List<String> = seedListings().flatMap { it.draft.photos }.distinct()
