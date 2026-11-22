package com.trinitybytes.minka.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Device cache of the listings a user has seen, so Inicio and "Mis publicaciones" still show their last
 * content without a connection (Figma 2.05). The server stays the source of truth.
 */
@Database(
    entities = [ListingEntity::class, FeedEntryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ListingConverters::class)
abstract class ItemsDatabase : RoomDatabase() {
    abstract fun listings(): ListingDao
}
