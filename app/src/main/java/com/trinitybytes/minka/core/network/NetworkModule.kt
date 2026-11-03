package com.trinitybytes.minka.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.ktor.client.HttpClient
import javax.inject.Singleton

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
        fun provideMockApi(routes: Set<@JvmSuppressWildcards MockRoute>): MockApi = MockApi(routes)

        @Provides
        @Singleton
        fun provideHttpClient(mockApi: MockApi): HttpClient = minkaHttpClient(mockApi.engine())
    }
}
