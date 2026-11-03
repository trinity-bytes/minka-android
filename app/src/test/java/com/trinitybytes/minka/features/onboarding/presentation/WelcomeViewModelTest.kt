package com.trinitybytes.minka.features.onboarding.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeViewModelTest {

    @Test
    fun `next advances one step and stops at the last one`() {
        val viewModel = WelcomeViewModel()
        assertEquals(0, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isLastStep)

        viewModel.next()
        assertEquals(1, viewModel.uiState.value.step)

        viewModel.next()
        viewModel.next()
        assertEquals(WelcomeUiState.LAST_STEP, viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.isLastStep)
    }

    @Test
    fun `skip jumps to the access step`() {
        val viewModel = WelcomeViewModel()

        viewModel.skip()

        assertTrue(viewModel.uiState.value.isLastStep)
    }

    @Test
    fun `swiping to a step makes it the current one`() {
        val viewModel = WelcomeViewModel()

        viewModel.onStepShown(1)
        assertEquals(1, viewModel.uiState.value.step)

        viewModel.onStepShown(7)
        assertEquals(1, viewModel.uiState.value.step)
    }
}
