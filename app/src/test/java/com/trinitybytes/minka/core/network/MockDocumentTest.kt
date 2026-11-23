package com.trinitybytes.minka.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

@Serializable
data class TestDb(val items: MutableList<String> = mutableListOf())

class MockDocumentTest {
    private val memory = InMemoryMockStorage()
    private var failWrites = false
    private val storage = object : MockStorage {
        override suspend fun read(key: String) = memory.read(key)
        override suspend fun write(key: String, value: String) { if (failWrites) throw IOException("full"); memory.write(key, value) }
    }
    private val doc = MockDocument(storage, "k", TestDb.serializer()) { TestDb() }

    @Test
    fun `a throwing change leaves the cache equal to the last persisted state`() = runTest {
        doc.update { it.items += "a" }
        try { doc.update { it.items += "b"; error("boom") }; fail() } catch (_: IllegalStateException) {}
        assertEquals(listOf("a"), doc.update { it.items.toList() })
        assertEquals("""{"items":["a"]}""", memory.read("k"))
    }

    @Test
    fun `a failed write leaves the cache equal to the last persisted state`() = runTest {
        doc.update { it.items += "a" }
        failWrites = true
        try { doc.update { it.items += "b" }; fail() } catch (_: IOException) {}
        failWrites = false
        assertEquals(listOf("a"), doc.update { it.items.toList() })
        assertEquals("""{"items":["a"]}""", memory.read("k"))
    }

    @Test
    fun `a failed first write keeps nothing`() = runTest {
        failWrites = true
        try { doc.update { it.items += "b" }; fail() } catch (_: IOException) {}
        failWrites = false
        assertEquals(emptyList<String>(), doc.update { it.items.toList() })
    }
}
