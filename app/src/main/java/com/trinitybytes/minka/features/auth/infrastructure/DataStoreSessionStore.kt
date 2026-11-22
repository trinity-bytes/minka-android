package com.trinitybytes.minka.features.auth.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.auth.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Keeps the signed-in session in DataStore Preferences (PLAN.md phase 2). */
class DataStoreSessionStore(private val dataStore: DataStore<Preferences>) : SessionStore {

    override val session: Flow<Session?> = dataStore.data.map { prefs ->
        val token = prefs[TOKEN] ?: return@map null
        Session(
            token = token,
            user = User(
                id = prefs[USER_ID].orEmpty(),
                name = prefs[USER_NAME].orEmpty(),
                contact = prefs[USER_CONTACT].orEmpty(),
            ),
        )
    }

    override suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[TOKEN] = session.token
            prefs[USER_ID] = session.user.id
            prefs[USER_NAME] = session.user.name
            prefs[USER_CONTACT] = session.user.contact
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> listOf(TOKEN, USER_ID, USER_NAME, USER_CONTACT).forEach(prefs::remove) }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_CONTACT = stringPreferencesKey("user_contact")
    }
}
