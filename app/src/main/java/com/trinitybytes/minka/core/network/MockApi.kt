package com.trinitybytes.minka.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The in-app "server" behind the Ktor client. Minka has no backend (PLAN.md section 4): every
 * request is answered by the [MockRoute] whose method and path match, after a short [latency]
 * so loading states are visible. While [connection] is offline, every request fails with an
 * [IOException], as a real client does without a network.
 */
class MockApi(
    private val routes: Set<MockRoute>,
    private val latency: Duration = DEFAULT_LATENCY,
    private val connection: MockConnection = MockConnection.Online,
) {

    fun engine(): HttpClientEngine = MockEngine { request ->
        delay(latency)
        if (connection.isOffline()) throw IOException("No connection (mock API switch)")
        val reply = dispatch(request)
        respond(
            content = reply.body,
            status = reply.status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private suspend fun dispatch(request: HttpRequestData): MockReply {
        val segments = request.url.segments.let { all ->
            if (all.take(BASE_SEGMENTS.size) == BASE_SEGMENTS) all.drop(BASE_SEGMENTS.size) else all
        }
        for (route in routes) {
            if (route.method != request.method) continue
            val params = route.match(segments) ?: continue
            return route.handler(MockCall(params, request.bodyText(), request.headers[HttpHeaders.Authorization]))
        }
        return MockReply.error(HttpStatusCode.NotFound, "not_found")
    }

    private fun HttpRequestData.bodyText(): String = when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        else -> ""
    }

    companion object {
        val DEFAULT_LATENCY = 600.milliseconds

        /** Path segments of [MINKA_API_BASE_URL]; routes are matched relative to them. */
        private val BASE_SEGMENTS = Url(MINKA_API_BASE_URL).segments
    }
}

/** One endpoint of the mock API. [path] is relative to the base URL; `{name}` segments are captured. */
class MockRoute(
    val method: HttpMethod,
    path: String,
    val handler: suspend (MockCall) -> MockReply,
) {
    private val pattern = path.trim('/').split('/')

    internal fun match(segments: List<String>): Map<String, String>? {
        if (segments.size != pattern.size) return null
        val params = mutableMapOf<String, String>()
        for ((expected, actual) in pattern.zip(segments)) {
            when {
                expected.startsWith('{') && expected.endsWith('}') ->
                    params[expected.substring(1, expected.length - 1)] = actual
                expected != actual -> return null
            }
        }
        return params
    }
}

/** What a [MockRoute] handler receives. */
class MockCall(
    private val pathParams: Map<String, String>,
    val bodyText: String,
    val authorization: String? = null,
) {
    fun pathParam(name: String): String = pathParams.getValue(name)

    inline fun <reified T> body(): T = MinkaJson.decodeFromString(bodyText)
}

/** What a [MockRoute] handler answers. */
class MockReply(val status: HttpStatusCode, val body: String) {
    companion object {
        inline fun <reified T> json(status: HttpStatusCode, value: T) =
            MockReply(status, MinkaJson.encodeToString(value))

        fun error(
            status: HttpStatusCode,
            code: String,
            retryAfterSeconds: Long? = null,
            attemptsLeft: Int? = null,
        ) = json(status, ApiError(code, retryAfterSeconds, attemptsLeft))
    }
}

/** Whether the mock API answers as if the device had no connection (Figma 1.22). */
fun interface MockConnection {
    fun isOffline(): Boolean

    companion object {
        val Online = MockConnection { false }
    }
}

/**
 * Debug switch: the connection is off while [flag] exists. On a debug build, from a computer:
 * `adb shell run-as com.trinitybytes.minka touch files/mock_offline` (and `rm` to turn it back on).
 */
class FileMockConnection(private val flag: File) : MockConnection {
    override fun isOffline(): Boolean = flag.exists()
}
