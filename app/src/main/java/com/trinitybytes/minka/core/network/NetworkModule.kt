package com.trinitybytes.minka.core.network

import android.content.Context
import android.content.pm.ApplicationInfo
import com.trinitybytes.minka.core.datastore.minkaPreferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.ktor.client.HttpClient
import java.io.File
import javax.inject.Singleton

/** File under `filesDir` whose presence turns the mock API offline on debug builds. */
const val MOCK_OFFLINE_FLAG = "mock_offline"

/**
 * Wires the Ktor client to the in-app [MockApi]. Each feature contributes its endpoints with
 * `@Provides @ElementsIntoSet fun routes(...): Set<MockRoute>`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Multibinds
    abstract fun mockRoutes(): Set<MockRoute>

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()

        @Provides
        @Singleton
        fun provideMockStorage(@ApplicationContext context: Context): MockStorage = DataStoreMockStorage(
            minkaPreferencesDataStore { context.preferencesDataStoreFile("mock_api") },
        )

        /** The "no connection" switch (Figma 1.22) only exists on debuggable builds. */
        @Provides
        @Singleton
        fun provideMockConnection(@ApplicationContext context: Context): MockConnection {
            val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            return if (debuggable) FileMockConnection(File(context.filesDir, MOCK_OFFLINE_FLAG)) else MockConnection.Online
        }

        @Provides
        @Singleton
        fun provideMockApi(routes: Set<@JvmSuppressWildcards MockRoute>, connection: MockConnection): MockApi =
            MockApi(routes, connection = connection)

        @Provides
        @Singleton
        fun provideHttpClient(mockApi: MockApi): HttpClient = minkaHttpClient(mockApi.engine())
    }
}
