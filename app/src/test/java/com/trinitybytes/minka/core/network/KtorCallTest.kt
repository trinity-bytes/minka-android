package com.trinitybytes.minka.core.network

import com.trinitybytes.minka.core.domain.Outcome
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration

class KtorCallTest {
    @Serializable data class Value(val text: String)

    @Test
    fun `maps success HTTP errors and transport errors and preserves cancellation`() = runTest {
        val routes = setOf(
            MockRoute(HttpMethod.Get, "ok") { MockReply.json(HttpStatusCode.OK, Value("yes")) },
            MockRoute(HttpMethod.Get, "bad") { MockReply.json(HttpStatusCode.BadRequest, ApiError("invalid")) },
        )
        val client = minkaHttpClient(MockApi(routes, latency = Duration.ZERO).engine())
        suspend fun call(path: String) = ktorCall<Value, String, String>(
            { it.text }, { it.body<ApiError>().code }, "offline", "unexpected",
        ) { client.get(path) }
        assertEquals(Outcome.Success("yes"), call("ok"))
        assertEquals(Outcome.Failure("invalid"), call("bad"))
        assertEquals(Outcome.Failure("offline"), ktorCall<Value, String, String>(
            { it.text }, { "error" }, "offline", "unexpected",
        ) { throw IOException() })
        try {
            ktorCall<Value, String, String>({ it.text }, { "error" }, "offline", "unexpected") {
                throw CancellationException("cancelled")
            }
            throw AssertionError("Cancellation was swallowed")
        } catch (_: CancellationException) { }
    }
}
