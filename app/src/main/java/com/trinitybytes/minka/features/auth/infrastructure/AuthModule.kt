package com.trinitybytes.minka.features.auth.infrastructure

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockStorage
import com.trinitybytes.minka.features.auth.domain.AuthRepository
import com.trinitybytes.minka.features.auth.domain.SessionStore
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
abstract class AuthModule {

    @Binds
    abstract fun bindAuthRepository(repository: KtorAuthRepository): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideSessionStore(@ApplicationContext context: Context): SessionStore = DataStoreSessionStore(
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") },
        )

        @Provides
        @Singleton
        fun provideMockAuthServer(storage: MockStorage, clock: Clock) = MockAuthServer(storage, clock)

        @Provides
        @ElementsIntoSet
        fun provideAuthRoutes(server: MockAuthServer): Set<MockRoute> = server.routes()
    }
}
