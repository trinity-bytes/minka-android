package com.trinitybytes.minka.core.designsystem.component

import com.trinitybytes.minka.features.auth.presentation.SignInUiState
import com.trinitybytes.minka.features.auth.presentation.withoutCountdownTicks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SignInProjectionTest {
    @Test fun countdownTicksDoNotChangeFormStateButLockTransitionsDo() {
        val initial = SignInUiState(contact = "ana", lockSecondsLeft = 60)
        assertEquals(initial.withoutCountdownTicks(), initial.copy(lockSecondsLeft = 59).withoutCountdownTicks())
        assertNotEquals(initial.withoutCountdownTicks(), initial.copy(lockSecondsLeft = null).withoutCountdownTicks())
        assertNotEquals(initial.withoutCountdownTicks(), initial.copy(contact = "other").withoutCountdownTicks())
    }
}
