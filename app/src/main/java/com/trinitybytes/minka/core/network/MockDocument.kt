package com.trinitybytes.minka.core.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer

/**
 * One JSON document of [MockStorage], changed one request at a time. Each change runs on a fresh copy
 * decoded from the last stored JSON and becomes current only once it is written, so a failed write
 * leaves memory and disk as they were.
 */
class MockDocument<T>(
    private val storage: MockStorage,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val initial: () -> T,
) {
    private val mutex = Mutex()

    /** The JSON last written (or read) for [key]; null until the first request. */
    private var stored: String? = null

    suspend fun <R> update(change: (T) -> R): R = mutex.withLock {
        val current = stored ?: storage.read(key)
        val document = current?.let { MinkaJson.decodeFromString(serializer, it) } ?: initial()
        val result = change(document)
        val next = MinkaJson.encodeToString(serializer, document)
        storage.write(key, next)
        stored = next
        result
    }
}
