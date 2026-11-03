package com.trinitybytes.minka.features.auth.application

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.domain.Registration
import com.trinitybytes.minka.features.auth.domain.TERMS_VERSION
import com.trinitybytes.minka.features.auth.presentation.FakeRegistrationRepository
import com.trinitybytes.minka.features.auth.presentation.TEST_CHALLENGE
import com.trinitybytes.minka.features.auth.presentation.TEST_CLOCK
import com.trinitybytes.minka.features.auth.presentation.TEST_NOW
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RegisterTest {

    private val repository = FakeRegistrationRepository()
    private val register = Register(repository, TEST_CLOCK)

    @Test
    fun `registering from the form stamps the current terms version and the time they were accepted`() = runTest {
        val result = register("Luis Mamani", "luis.mamani@gmail.com", "Trueque2026!")

        assertEquals(Outcome.Success(TEST_CHALLENGE), result)
        assertEquals(
            listOf(Registration("Luis Mamani", "luis.mamani@gmail.com", "Trueque2026!", TERMS_VERSION, TEST_NOW)),
            repository.registrations,
        )
    }

    @Test
    fun `a full registration is still passed through unchanged`() = runTest {
        val registration = Registration(
            name = "Luis Mamani",
            contact = "987111222",
            password = "Trueque2026!",
            termsVersion = "0.9",
            termsAcceptedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        val result = register(registration)

        assertEquals(Outcome.Success(TEST_CHALLENGE), result)
        assertEquals(listOf(registration), repository.registrations)
    }
}
