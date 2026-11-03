package com.trinitybytes.minka.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The JSON format shared by the client and the mock API. */
val MinkaJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Base URL of the API. Nothing listens there: the [MockApi] engine answers every request. */
const val MINKA_API_BASE_URL = "https://api.minka.app/v1/"

/** Builds the app's HTTP client on [engine]; a real server would only need a different engine. */
fun minkaHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(MinkaJson) }
    defaultRequest { url(MINKA_API_BASE_URL) }
}

/** Error body of every non-2xx answer. [code] is stable and mapped to domain errors by adapters. */
@Serializable
data class ApiError(
    val code: String,
    val retryAfterSeconds: Long? = null,
)
