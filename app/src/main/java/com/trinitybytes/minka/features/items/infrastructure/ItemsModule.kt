package com.trinitybytes.minka.features.items.infrastructure

import android.content.Context
import androidx.room.Room
import com.trinitybytes.minka.core.database.ItemsDatabase
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockSessions
import com.trinitybytes.minka.core.network.MockUserNames
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.features.items.domain.ItemsRepository
import com.trinitybytes.minka.features.onboarding.infrastructure.MockProfileServer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ItemsModule {

    @Binds
    abstract fun bindItemsRepository(repository: KtorItemsRepository): ItemsRepository

    @Binds
    abstract fun bindListingCache(cache: RoomListingCache): ListingCache

    companion object {
        private const val DATABASE_NAME = "items.db"

        @Provides
        @Singleton
        fun provideItemsDatabase(@ApplicationContext context: Context): ItemsDatabase =
            Room.databaseBuilder(context, ItemsDatabase::class.java, DATABASE_NAME).build()

        @Provides
        fun provideViewerProfiles(server: MockProfileServer): ViewerProfiles = ViewerProfiles(server::profileOf)

        @Provides
        @Singleton
        fun provideMockItemsServer(
            storage: MockStorage,
            sessions: MockSessions,
            names: MockUserNames,
            clock: Clock,
            profiles: ViewerProfiles,
        ) = MockItemsServer(storage, sessions, clock, names, profiles)

        @Provides
        @ElementsIntoSet
        fun provideItemsRoutes(server: MockItemsServer): Set<MockRoute> = server.routes()
    }
}
