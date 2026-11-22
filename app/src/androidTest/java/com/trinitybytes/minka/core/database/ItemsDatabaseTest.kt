package com.trinitybytes.minka.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemsDatabaseTest {
    private lateinit var database: ItemsDatabase
    private lateinit var dao: ListingDao

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ItemsDatabase::class.java,
        ).build()
        dao = database.listings()
    }

    @After
    fun close() {
        database.close()
    }

    private fun listing(
        viewer: String,
        id: String,
        owner: String = "someone",
        status: String = "ACTIVE",
        updatedAt: Long = 1,
        photos: List<String> = listOf("cover", "second"),
    ) = ListingEntity(
        viewerId = viewer,
        id = id,
        ownerId = owner,
        ownerName = "Owner",
        ownerDistrict = "Miraflores",
        ownerMemberSince = null,
        photos = photos,
        title = "Title $id",
        category = "HOME",
        condition = "GOOD",
        wants = listOf("BOOKS", "PLANTS"),
        description = null,
        estimatedValue = null,
        district = "Miraflores",
        meetingPoint = null,
        hideExactLocation = true,
        homeDelivery = false,
        status = status,
        createdAt = 1,
        updatedAt = updatedAt,
        distanceKm = 1.5,
    )

    @Test
    fun listingKeepsPhotoOrderAndWantsThroughTheConverters() = runBlocking {
        dao.upsert(listOf(listing("ana", "a")))
        val stored = dao.listing("ana", "a")!!
        assertEquals(listOf("cover", "second"), stored.photos)
        assertEquals(listOf("BOOKS", "PLANTS"), stored.wants)
    }

    @Test
    fun feedReturnsEachShelfInServerOrderForTheViewerOnly() = runBlocking {
        dao.upsert(listOf(listing("ana", "a"), listing("ana", "b"), listing("other", "a")))
        dao.insertFeedEntries(
            listOf(
                FeedEntryEntity("ana", "NEARBY", "b", 0),
                FeedEntryEntity("ana", "NEARBY", "a", 1),
                FeedEntryEntity("ana", "BY_INTEREST", "a", 0),
                FeedEntryEntity("other", "NEARBY", "a", 0),
            ),
        )
        assertEquals(listOf("b", "a"), dao.feed("ana", "NEARBY").map { it.id })
        assertEquals(listOf("a"), dao.feed("ana", "BY_INTEREST").map { it.id })
        dao.clearFeed("ana")
        assertEquals(emptyList<ListingEntity>(), dao.feed("ana", "NEARBY"))
        assertEquals(listOf("a"), dao.feed("other", "NEARBY").map { it.id })
    }

    @Test
    fun ownListingsAreFilteredByStatusAndOrderedByMostRecentChange() = runBlocking {
        dao.upsert(
            listOf(
                listing("ana", "old", owner = "ana", updatedAt = 1),
                listing("ana", "new", owner = "ana", updatedAt = 2),
                listing("ana", "paused", owner = "ana", status = "PAUSED"),
                listing("ana", "foreign"),
            ),
        )
        assertEquals(listOf("new", "old"), dao.own("ana", "ACTIVE").map { it.id })
        assertEquals(listOf("paused"), dao.own("ana", "PAUSED").map { it.id })
    }

    @Test
    fun pruningOwnKeepsTheReturnedListingsAndLeavesOtherStatuses() = runBlocking {
        dao.upsert(
            listOf(
                listing("ana", "keep", owner = "ana"),
                listing("ana", "gone", owner = "ana"),
                listing("ana", "paused", owner = "ana", status = "PAUSED"),
            ),
        )
        dao.pruneOwn("ana", "ACTIVE", listOf("keep"))
        assertEquals(listOf("keep"), dao.own("ana", "ACTIVE").map { it.id })
        assertEquals(listOf("paused"), dao.own("ana", "PAUSED").map { it.id })
        dao.pruneOwn("ana", "ACTIVE", emptyList())
        assertEquals(emptyList<ListingEntity>(), dao.own("ana", "ACTIVE"))
    }

    @Test
    fun pruningForeignDropsListingsOutsideTheFeedButNeverOwnOnes() = runBlocking {
        dao.upsert(listOf(listing("ana", "shown"), listing("ana", "stale"), listing("ana", "mine", owner = "ana")))
        dao.insertFeedEntries(listOf(FeedEntryEntity("ana", "NEARBY", "shown", 0)))
        dao.pruneForeign("ana")
        assertEquals("shown", dao.listing("ana", "shown")?.id)
        assertNull(dao.listing("ana", "stale"))
        assertEquals("mine", dao.listing("ana", "mine")?.id)
    }

    @Test
    fun deleteAndRemoveFromFeedAffectOnlyTheViewer() = runBlocking {
        dao.upsert(listOf(listing("ana", "a"), listing("other", "a")))
        dao.insertFeedEntries(listOf(FeedEntryEntity("ana", "NEARBY", "a", 0)))
        dao.removeFromFeed("ana", "a")
        dao.delete("ana", "a")
        assertNull(dao.listing("ana", "a"))
        assertEquals(emptyList<ListingEntity>(), dao.feed("ana", "NEARBY"))
        assertEquals("a", dao.listing("other", "a")?.id)
    }
}
