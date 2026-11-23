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
    private val migrate: (T) -> Boolean = { false },
    private val initial: () -> T,
) {
    private val mutex = Mutex()

    /** The JSON last written (or read) for [key]; null until the first request. */
    private var stored: String? = null

    suspend fun <R> read(block: (T) -> R): R = mutex.withLock {
        block(load())
    }

    suspend fun <R> update(change: (T) -> R): R = mutex.withLock {
        val document = load()
        val result = change(document)
        val next = MinkaJson.encodeToString(serializer, document)
        storage.write(key, next)
        stored = next
        result
    }

    private suspend fun load(): T {
        val cached = stored
        if (cached != null) return MinkaJson.decodeFromString(serializer, cached)
        val current = storage.read(key) ?: return initial()
        val document = MinkaJson.decodeFromString(serializer, current)
        val next = if (migrate(document)) MinkaJson.encodeToString(serializer, document) else current
        if (next != current) storage.write(key, next)
        stored = next
        return document
    }
}
