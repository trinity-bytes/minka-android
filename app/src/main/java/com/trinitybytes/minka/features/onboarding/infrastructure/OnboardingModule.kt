package com.trinitybytes.minka.features.onboarding.infrastructure

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.trinitybytes.minka.core.datastore.minkaPreferencesDataStore
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockSessions
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.features.onboarding.domain.LocationProvider
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import com.trinitybytes.minka.features.onboarding.domain.PermissionPolicy
import com.trinitybytes.minka.features.onboarding.domain.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    abstract fun bindProfileRepository(repository: KtorProfileRepository): ProfileRepository

    companion object {
        @Provides
        @Singleton
        fun provideProgressStore(@ApplicationContext context: Context): OnboardingProgressStore =
            DataStoreOnboardingProgressStore(
                minkaPreferencesDataStore { context.preferencesDataStoreFile("onboarding") },
            )

        @Provides
        fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider =
            AndroidLocationProvider(context)

        @Provides
        fun providePermissionPolicy(): PermissionPolicy = AndroidPermissionPolicy()

        @Provides
        @Singleton
        fun provideMockProfileServer(
            storage: MockStorage,
            sessions: MockSessions,
        ) = MockProfileServer(storage, sessions)

        @Provides
        @ElementsIntoSet
        fun provideProfileRoutes(server: MockProfileServer): Set<MockRoute> = server.routes()
    }
}
