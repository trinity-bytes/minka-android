package com.trinitybytes.minka.features.onboarding.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Keeps the profile setup progress in DataStore Preferences (S4), in its own file next to the
 * session: the user it belongs to and the next [SetupStep] name, or [COMPLETED].
 */
class DataStoreOnboardingProgressStore(private val dataStore: DataStore<Preferences>) : OnboardingProgressStore {

    override val progress: Flow<OnboardingProgress?> = dataStore.data.map { prefs ->
        val userId = prefs[USER_ID] ?: return@map null
        when (val step = prefs[NEXT_STEP]) {
            COMPLETED -> OnboardingProgress.Completed(userId)
            else -> SetupStep.entries.firstOrNull { it.name == step }?.let { OnboardingProgress.Pending(userId, it) }
        }
    }

    override suspend fun save(progress: OnboardingProgress) {
        dataStore.edit { prefs ->
            prefs[USER_ID] = progress.userId
            prefs[NEXT_STEP] = when (progress) {
                is OnboardingProgress.Pending -> progress.nextStep.name
                is OnboardingProgress.Completed -> COMPLETED
            }
        }
    }

    private companion object {
        const val COMPLETED = "completed"
        val USER_ID = stringPreferencesKey("onboarding_user_id")
        val NEXT_STEP = stringPreferencesKey("onboarding_next_step")
    }
}
