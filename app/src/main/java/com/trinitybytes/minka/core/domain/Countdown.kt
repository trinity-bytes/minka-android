package com.trinitybytes.minka.core.domain

import java.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits the remaining whole seconds immediately and then once per second, including zero. */
fun countdown(deadlineMillis: Long, clock: Clock): Flow<Long> = flow {
    while (true) {
        val remaining = ((deadlineMillis - clock.millis()).coerceAtLeast(0) + 999) / 1_000
        emit(remaining)
        if (remaining == 0L) break
        delay(1_000)
    }
}
