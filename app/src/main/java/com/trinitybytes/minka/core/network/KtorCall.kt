package com.trinitybytes.minka.core.network

import com.trinitybytes.minka.core.domain.Outcome
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Maps transport and HTTP failures without swallowing coroutine cancellation. */
suspend inline fun <reified D, T, E> ktorCall(
    crossinline toDomain: (D) -> T,
    crossinline httpError: suspend (HttpResponse) -> E,
    offline: E,
    unexpected: E,
    crossinline request: suspend () -> HttpResponse,
): Outcome<T, E> = try {
    val response = request()
    if (response.status.isSuccess()) Outcome.Success(toDomain(response.body<D>()))
    else Outcome.Failure(httpError(response))
} catch (e: CancellationException) {
    throw e
} catch (_: IOException) {
    Outcome.Failure(offline)
} catch (_: Exception) {
    Outcome.Failure(unexpected)
}
