package com.trinitybytes.minka.features.auth.presentation

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.domain.AuthError
import com.trinitybytes.minka.features.auth.domain.AuthOutcome

/** Whether the request failed for lack of connection, which the screens show as Figma 1.22. */
internal fun AuthOutcome<*>.isOffline(): Boolean = this is Outcome.Failure && error == AuthError.Offline
