package com.trinitybytes.minka.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/** Where the mock API keeps its "server" data (one JSON document per key), so it survives restarts. */
interface MockStorage {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
}

class DataStoreMockStorage(private val dataStore: DataStore<Preferences>) : MockStorage {
    override suspend fun read(key: String): String? = dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun write(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }
}

class InMemoryMockStorage : MockStorage {
    private val documents = mutableMapOf<String, String>()

    override suspend fun read(key: String): String? = documents[key]

    override suspend fun write(key: String, value: String) {
        documents[key] = value
    }
}
