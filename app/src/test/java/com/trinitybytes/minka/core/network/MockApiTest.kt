package com.trinitybytes.minka.core.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.time.Duration

class MockApiTest {

    @Serializable
    data class Echo(val value: String)

    private val routes = setOf(
        MockRoute(HttpMethod.Get, "items/{id}") { call ->
            MockReply.json(HttpStatusCode.OK, Echo(call.pathParam("id")))
        },
        MockRoute(HttpMethod.Post, "echo") { call ->
            MockReply.json(HttpStatusCode.Created, call.body<Echo>())
        },
    )

    private val client = minkaHttpClient(MockApi(routes, latency = Duration.ZERO).engine())

    @Test
    fun `a matching route answers with its handler and path parameters`() = runTest {
        val response = client.get("items/42")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(Echo("42"), response.body<Echo>())
    }

    @Test
    fun `the request body reaches the handler as json`() = runTest {
        val response = client.post("echo") {
            contentType(ContentType.Application.Json)
            setBody(Echo("hola"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(Echo("hola"), response.body<Echo>())
    }

    @Test
    fun `an unknown route or method answers 404 with an error code`() = runTest {
        val unknownPath = client.get("nope")
        val wrongMethod = client.post("items/42")

        assertEquals(HttpStatusCode.NotFound, unknownPath.status)
        assertEquals(HttpStatusCode.NotFound, wrongMethod.status)
        assertEquals(ApiError("not_found"), unknownPath.body<ApiError>())
        assertEquals("""{"code":"not_found"}""", wrongMethod.bodyAsText())
    }

    @Test
    fun `while the no-connection switch is on every request fails as offline`() = runTest {
        var offline = true
        val client = minkaHttpClient(MockApi(routes, Duration.ZERO, MockConnection { offline }).engine())

        val failure = runCatching { client.get("items/42") }.exceptionOrNull()
        assertTrue("got $failure", failure is IOException)

        offline = false
        assertEquals(Echo("42"), client.get("items/42").body<Echo>())
    }

    @Test
    fun `the debug switch file turns the connection off while it exists`() {
        val flag = File(Files.createTempDirectory("mock-connection").toFile(), "mock_offline")
        val connection = FileMockConnection(flag)
        assertFalse(connection.isOffline())

        flag.createNewFile()
        assertTrue(connection.isOffline())

        flag.delete()
        assertFalse(connection.isOffline())
        flag.parentFile?.delete()
    }
}
